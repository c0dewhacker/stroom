package stroom.data.store.impl.fs;

import stroom.cluster.lock.api.ClusterLockService;
import stroom.data.store.impl.fs.shared.FindFsVolumeCriteria;
import stroom.data.store.impl.fs.shared.FsVolume;
import stroom.data.store.impl.fs.shared.FsVolume.VolumeUseStatus;
import stroom.data.store.impl.fs.shared.FsVolumeState;
import stroom.docref.DocRef;
import stroom.node.api.NodeInfo;
import stroom.security.api.SecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.statistics.api.InternalStatisticEvent;
import stroom.statistics.api.InternalStatisticKey;
import stroom.statistics.api.InternalStatisticsReceiver;
import stroom.task.api.TaskContext;
import stroom.util.AuditUtil;
import stroom.util.NullSafe;
import stroom.util.date.DateUtil;
import stroom.util.entityevent.EntityAction;
import stroom.util.entityevent.EntityEvent;
import stroom.util.entityevent.EntityEventBus;
import stroom.util.entityevent.EntityEventHandler;
import stroom.util.io.FileUtil;
import stroom.util.io.PathCreator;
import stroom.util.io.capacity.HasCapacitySelector;
import stroom.util.io.capacity.HasCapacitySelectorFactory;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.shared.Clearable;
import stroom.util.shared.Flushable;
import stroom.util.shared.ResultPage;
import stroom.util.sysinfo.HasSystemInfo;
import stroom.util.sysinfo.SystemInfoResult;
import stroom.util.time.StroomDuration;

import com.google.common.collect.ImmutableSortedMap;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
@EntityEventHandler(type = FsVolumeService.ENTITY_TYPE, action = {
        EntityAction.CREATE,
        EntityAction.DELETE})
public class FsVolumeService implements EntityEvent.Handler, Clearable, Flushable, HasSystemInfo {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(FsVolumeService.class);

    private static final String LOCK_NAME = "REFRESH_FS_VOLUMES";
    static final String ENTITY_TYPE = "FILE_SYSTEM_VOLUME";
    private static final DocRef EVENT_DOCREF = new DocRef(ENTITY_TYPE, null, null);

    private final FsVolumeDao fsVolumeDao;
    private final FsVolumeStateDao fileSystemVolumeStateDao;
    private final SecurityContext securityContext;
    private final Provider<FsVolumeConfig> volumeConfigProvider;
    private final InternalStatisticsReceiver statisticsReceiver;
    private final ClusterLockService clusterLockService;
    private final Provider<EntityEventBus> entityEventBusProvider;
    private final PathCreator pathCreator;
    // Hold a cache of the current picture of available volumes, with their used/free/total/etc. stats.
    // Allows for fast volume selection without having to hit the db each time.
    private final AtomicReference<VolumeList> currentVolumeList = new AtomicReference<>();
    private final AtomicReference<HasCapacitySelector> volumeSelector = new AtomicReference<>();
    private final NodeInfo nodeInfo;
    private final TaskContext taskContext;
    private final HasCapacitySelectorFactory hasCapacitySelectorFactory;

    private volatile boolean createdDefaultVolumes;
    private volatile boolean creatingDefaultVolumes;

    @Inject
    public FsVolumeService(final FsVolumeDao fsVolumeDao,
                           final FsVolumeStateDao fileSystemVolumeStateDao,
                           final SecurityContext securityContext,
                           final Provider<FsVolumeConfig> volumeConfigProvider,
                           final InternalStatisticsReceiver statisticsReceiver,
                           final ClusterLockService clusterLockService,
                           final Provider<EntityEventBus> entityEventBusProvider,
                           final PathCreator pathCreator,
                           final NodeInfo nodeInfo,
                           final TaskContext taskContext,
                           final HasCapacitySelectorFactory hasCapacitySelectorFactory) {
        this.fsVolumeDao = fsVolumeDao;
        this.fileSystemVolumeStateDao = fileSystemVolumeStateDao;
        this.securityContext = securityContext;
        this.volumeConfigProvider = volumeConfigProvider;
        this.statisticsReceiver = statisticsReceiver;
        this.clusterLockService = clusterLockService;
        this.entityEventBusProvider = entityEventBusProvider;
        this.pathCreator = pathCreator;
        this.nodeInfo = nodeInfo;
        this.taskContext = taskContext;
        this.hasCapacitySelectorFactory = hasCapacitySelectorFactory;

        ensureDefaultVolumes();
    }

    public FsVolume create(final FsVolume fileVolume) {
        return securityContext.secureResult(PermissionNames.MANAGE_VOLUMES_PERMISSION, () -> {
            FsVolume result = null;
            String pathString = fileVolume.getPath();
            try {
                if (pathString != null) {
                    Path path = Paths.get(pathString);
                    if (Files.isDirectory(path)) {
                        final long count = FileUtil.count(path);
                        if (count > 0) {
                            throw new IOException("Attempt to create volume in a directory that is not empty: " + path);
                        }
                    }

                    Files.createDirectories(path);
                    LOGGER.info(() -> LogUtil.message("Creating volume in {}", pathString));

                    if (fileVolume.getByteLimit() == null) {
                        //set an arbitrary default limit size of 250MB on each volume to prevent the
                        //filesystem from running out of space, assuming they have 500MB free of course.
                        getDefaultVolumeLimit(path).ifPresent(fileVolume::setByteLimit);
                    }
                }
                fileVolume.setStatus(FsVolume.VolumeUseStatus.ACTIVE);

                final FsVolumeState fileVolumeState = fileSystemVolumeStateDao.create(new FsVolumeState());
                fileVolume.setVolumeState(fileVolumeState);

                AuditUtil.stamp(securityContext.getUserId(), fileVolume);
                result = fsVolumeDao.create(fileVolume);
                result.setVolumeState(fileVolume.getVolumeState());
            } catch (IOException e) {
                LOGGER.error("Unable to create volume due to an error creating directory {}", pathString, e);
            }

            fireChange(EntityAction.CREATE);

            return result;
        });
    }

    public FsVolume update(final FsVolume fileVolume) {
        return securityContext.secureResult(PermissionNames.MANAGE_VOLUMES_PERMISSION, () -> {
            AuditUtil.stamp(securityContext.getUserId(), fileVolume);
            final FsVolume result = fsVolumeDao.update(fileVolume);

            fireChange(EntityAction.UPDATE);

            return result;
        });
    }

    public int delete(final int id) {
        return securityContext.secureResult(PermissionNames.MANAGE_VOLUMES_PERMISSION, () -> {
            final int result = fsVolumeDao.delete(id);

            fireChange(EntityAction.DELETE);

            return result;
        });
    }

    public FsVolume fetch(final int id) {
        return securityContext.secureResult(PermissionNames.MANAGE_VOLUMES_PERMISSION, () -> fsVolumeDao.fetch(id));
    }

    public ResultPage<FsVolume> find(final FindFsVolumeCriteria criteria) {
        return doFind(criteria);
    }

    private ResultPage<FsVolume> doFind(final FindFsVolumeCriteria criteria) {
        return fsVolumeDao.find(criteria);
    }

    FsVolume getVolume() {
        return securityContext.insecureResult(() -> {
            final Set<FsVolume> set = getVolumeSet(VolumeUseStatus.ACTIVE);
            if (set.size() > 0) {
                final FsVolume volume = set.iterator().next();
                LOGGER.trace("Using volume {}", volume);
                return volume;
            }
            return null;
        });
    }

    private Set<FsVolume> getVolumeSet(final VolumeUseStatus streamStatus) {
        final HasCapacitySelector volumeSelector = getVolumeSelector();
        final List<FsVolume> allVolumeList = getCurrentVolumeList().list;
        LOGGER.trace("allVolumeList {}", allVolumeList);
        final List<FsVolume> freeVolumes = FsVolumeListUtil.removeFullVolumes(allVolumeList);
        LOGGER.trace("freeVolumes {}", freeVolumes);
        Set<FsVolume> set = Collections.emptySet();

        final List<FsVolume> filteredVolumeList = getFilteredVolumeList(freeVolumes, streamStatus);
        if (filteredVolumeList.size() > 0) {
            set = Collections.singleton(volumeSelector.select(filteredVolumeList));
        }

        if (set.isEmpty()) {
            LOGGER.warn("No {} volume found, all vols: {}, non-full vols: {}, non-full {} vols: {}",
                    streamStatus,
                    allVolumeList.size(),
                    freeVolumes.size(),
                    streamStatus,
                    filteredVolumeList.size());
        }

        return set;
    }

    private List<FsVolume> getFilteredVolumeList(final List<FsVolume> allVolumes, final VolumeUseStatus streamStatus) {
        final List<FsVolume> list = new ArrayList<>();
        for (final FsVolume volume : allVolumes) {
            // Check the volume type matches.
            boolean ok = true;

            // Check the stream volume use status matches.
            if (streamStatus != null) {
                ok = streamStatus.equals(volume.getStatus());
            }

            if (ok) {
                list.add(volume);
            }
        }
        return list;
    }

    private HasCapacitySelector getVolumeSelector() {
        String requiredSelectorName = null;

        try {
            requiredSelectorName = volumeConfigProvider.get().getVolumeSelector();
        } catch (final RuntimeException e) {
            LOGGER.debug(e::getMessage);
        }

        final HasCapacitySelector currentSelector = volumeSelector.get();
        if (currentSelector != null
                && requiredSelectorName != null
                && currentSelector.getName().equals(requiredSelectorName)) {
            return currentSelector;
        } else {
            final String requiredSelectorNameCopy = requiredSelectorName;
            // Atomically update the selector reference to the new one
            return volumeSelector.accumulateAndGet(
                    null,
                    (curr, next) -> {
                        if (curr == null
                                || requiredSelectorNameCopy == null
                                || !curr.getName().equals(requiredSelectorNameCopy)) {
                            return hasCapacitySelectorFactory.createSelectorOrDefault(
                                    requiredSelectorNameCopy);
                        } else {
                            // Existing one is ok, maybe another thread did it
                            return curr;
                        }
                    });
        }
    }

    @Override
    public void onChange(final EntityEvent event) {
        LOGGER.debug("Clearing currentVolumeList");
        currentVolumeList.set(null);
    }

    private void fireChange(final EntityAction action) {
        currentVolumeList.set(null);
        if (entityEventBusProvider != null) {
            try {
                final EntityEventBus entityEventBus = entityEventBusProvider.get();
                if (entityEventBus != null) {
                    entityEventBus.fire(new EntityEvent(EVENT_DOCREF, action));
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e::getMessage, e);
            }
        }
    }

    @Override
    public void clear() {
        final List<FsVolume> volumeList = doFind(FindFsVolumeCriteria.matchAll()).getValues();
        for (final FsVolume volume : volumeList) {
            final String path = volume.getPath();
            FileUtil.deleteDir(Paths.get(path));
        }

        // Delete default volumes.
        LOGGER.info(() -> "Deleting default volumes");
        if (volumeConfigProvider.get().getDefaultStreamVolumePaths() != null) {
            final List<String> paths = volumeConfigProvider.get().getDefaultStreamVolumePaths();
            for (String path : paths) {
                final Path resolvedPath = Paths.get(
                        pathCreator.makeAbsolute(
                                pathCreator.replaceSystemProperties(path)));
                LOGGER.info("Deleting directory {}", resolvedPath.toAbsolutePath().normalize().toString());
                FileUtil.deleteDir(resolvedPath);
            }
        }

        // Clear state between tests.
        currentVolumeList.set(null);
        createdDefaultVolumes = false;
    }

    private VolumeList getCurrentVolumeList() {
        VolumeList volumeList = currentVolumeList.get();
        if (volumeList == null) {
            synchronized (this) {
                volumeList = currentVolumeList.get();
                if (volumeList == null) {
                    volumeList = refresh(true);
                    currentVolumeList.set(volumeList);
                }
            }
        }
        return volumeList;
    }

    @Override
    public void flush() {
        // Called from UI so make sure it is up to date
        refresh(true);
    }

    void updateStatus() {
        // Each node needs to get a lock so that the first one in can update the state and then
        // every other node can then just read the state written by the first node.
        clusterLockService.lock(LOCK_NAME, () ->
                refresh(false));
    }

    private synchronized VolumeList refresh(final boolean isForcedRefresh) {
        taskContext.info(() -> "Refreshing volumes");
        final Instant now = Instant.now();
        final List<FsVolume> volumes = new ArrayList<>();

        final FindFsVolumeCriteria findVolumeCriteria = FindFsVolumeCriteria.matchAll();
        findVolumeCriteria.addSort(FindFsVolumeCriteria.FIELD_ID, false, false);
        final List<FsVolume> dbVolumes = find(findVolumeCriteria).getValues();

        final StroomDuration volumeStateUpdateThreshold = volumeConfigProvider.get().getMaxVolumeStateAge();
        final long updateTimeCutOffEpochMs = now.minus(volumeStateUpdateThreshold.getDuration()).toEpochMilli();

        // Get the oldest update time from the list then use that to see if we need to update all
        final Optional<Long> optMinUpdateTimeEpochMs = dbVolumes.stream()
                .map(vol -> NullSafe.get(vol, FsVolume::getVolumeState, FsVolumeState::getUpdateTimeMs))
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder());

        if (optMinUpdateTimeEpochMs.isEmpty()
                || isForcedRefresh
                || optMinUpdateTimeEpochMs.get() < updateTimeCutOffEpochMs) {
            for (final FsVolume volume : dbVolumes) {
                taskContext.info(() -> "Refreshing volume '" + volume.getPath() + "'");
                // Update the volume state and save in the DB.
                updateVolumeState(volume);

                // Record some statistics for the use of this volume.
                recordStats(volume);
                volumes.add(volume);
            }
        } else {
            LOGGER.debug(() -> LogUtil.message("Not updating state for vols {}, with min update time {}",
                    optMinUpdateTimeEpochMs.map(DateUtil::createNormalDateTimeString)));
            volumes.addAll(dbVolumes);
        }

        final VolumeList newList = new VolumeList(now.toEpochMilli(), volumes);
        final VolumeList currentList = currentVolumeList.get();
        if (currentList == null || currentList.createTime < newList.createTime) {
            currentVolumeList.set(newList);
        }

        return newList;
    }

    private void recordStats(final FsVolume volume) {
        if (statisticsReceiver != null) {
            try {
                final FsVolumeState volumeState = volume.getVolumeState();

                final long now = System.currentTimeMillis();
                final List<InternalStatisticEvent> events = new ArrayList<>();
                addStatisticEvent(events, now, volume, "Limit", volume.getByteLimit());
                addStatisticEvent(events, now, volume, "Used", volumeState.getBytesUsed());
                addStatisticEvent(events, now, volume, "Free", volumeState.getBytesFree());
                addStatisticEvent(events, now, volume, "Total", volumeState.getBytesTotal());
                statisticsReceiver.putEvents(events);
            } catch (final RuntimeException e) {
                LOGGER.warn(e::getMessage);
                LOGGER.debug(e::getMessage, e);
            }
        }
    }

    private void addStatisticEvent(final List<InternalStatisticEvent> events,
                                   final long timeMs,
                                   final FsVolume volume,
                                   final String type,
                                   final Long bytes) {
        if (bytes != null) {
            SortedMap<String, String> tags = ImmutableSortedMap.<String, String>naturalOrder()
                    .put("Id", String.valueOf(volume.getId()))
                    .put("Path", volume.getPath())
                    .put("Type", type)
                    .put("Node", nodeInfo.getThisNodeName())
                    .build();

            InternalStatisticEvent event = InternalStatisticEvent.createValueStat(
                    InternalStatisticKey.VOLUMES, timeMs, tags, bytes.doubleValue());
            events.add(event);
        }
    }

    private void updateVolumeState(final FsVolume volume) {
        final Path path = Paths.get(volume.getPath());

        try {
            FsVolumeState volumeState = volume.getVolumeState();
            volumeState.setUpdateTimeMs(System.currentTimeMillis());

            // Ensure the path exists
            if (Files.isDirectory(path)) {
                LOGGER.debug(() -> LogUtil.message("updateVolumeState() path exists: {}", path));
                setSizes(path, volume, volumeState);
            } else {
                Files.createDirectories(path);
                LOGGER.debug(() -> LogUtil.message("updateVolumeState() path created: {}", path));
                setSizes(path, volume, volumeState);
            }

            volumeState = saveVolumeState(volumeState);
            volume.setVolumeState(volumeState);

            LOGGER.debug(() -> LogUtil.message("updateVolumeState() exit {}", volume));

        } catch (final IOException | RuntimeException e) {
            LOGGER.error(() -> LogUtil.message("updateVolumeState() path not created: {}", path));
        }
    }

    private void setSizes(final Path path,
                          final FsVolume fsVolume,
                          final FsVolumeState volumeState) throws IOException {
        final FileStore fileStore = Files.getFileStore(path);
        final long osUsableSpace = fileStore.getUsableSpace();
        final long osFreeSpace = fileStore.getUnallocatedSpace();
        final long totalSpace = fileStore.getTotalSpace();
        final long usedSpace = totalSpace - osFreeSpace;
        // Calc free space based on the limit if one is set
        final long freeSpace = fsVolume.getCapacityLimitBytes()
                .stream()
                .map(limit -> Math.max(limit - usedSpace, 0))
                .findAny()
                .orElse(osUsableSpace);

        volumeState.setBytesTotal(totalSpace);
        volumeState.setBytesFree(freeSpace);
        volumeState.setBytesUsed(usedSpace);
    }

    private FsVolumeState saveVolumeState(final FsVolumeState volumeState) {
        // If another node updates the state at the same time it doesn't matter
        // as they will be updating to the same value. This saves having to
        // get a cluster lock
        return fileSystemVolumeStateDao.updateWithoutOptimisticLocking(volumeState);
    }

    private void ensureDefaultVolumes() {
        if (!createdDefaultVolumes) {
            securityContext.asProcessingUser(() -> createDefaultVolumes());
        }
    }

    private synchronized void createDefaultVolumes() {
        if (!createdDefaultVolumes && !creatingDefaultVolumes) {
            try {
                creatingDefaultVolumes = true;
                securityContext.insecure(() -> {
                    final FsVolumeConfig volumeConfig = volumeConfigProvider.get();
                    final boolean isEnabled = volumeConfig.isCreateDefaultStreamVolumesOnStart();
                    if (isEnabled) {
                        final FindFsVolumeCriteria findVolumeCriteria = FindFsVolumeCriteria.matchAll();
                        findVolumeCriteria.addSort(FindFsVolumeCriteria.FIELD_ID, false, false);
                        final List<FsVolume> existingVolumes = doFind(findVolumeCriteria).getValues();
                        if (existingVolumes.size() == 0) {
                            if (volumeConfig.getDefaultStreamVolumePaths() != null) {
                                final List<String> paths = volumeConfig.getDefaultStreamVolumePaths();
                                for (String path : paths) {
                                    path = pathCreator.replaceSystemProperties(path);
                                    path = pathCreator.makeAbsolute(path);
                                    final Path resolvedPath = Paths.get(path.trim());
                                    LOGGER.info("Creating default data volume with path {}",
                                            resolvedPath.toAbsolutePath().normalize());

                                    createVolume(resolvedPath);
                                }

                            } else {
                                LOGGER.warn(() -> "No suitable directory to create default volumes in");
                            }
                        } else {
                            LOGGER.info(() -> "Existing volumes exist, won't create default volumes");
                        }
                    } else {
                        LOGGER.info(() -> "Creation of default volumes is currently disabled");
                    }
                });
            } catch (final RuntimeException e) {
                LOGGER.error(e::getMessage, e);
            } finally {
                createdDefaultVolumes = true;
                creatingDefaultVolumes = false;
            }
        }
    }

    private void createVolume(final Path path) {
        final FsVolume fileVolume = new FsVolume();
        fileVolume.setPath(FileUtil.getCanonicalPath(path));
        create(fileVolume);

//        String pathStr = FileUtil.getCanonicalPath(path);
//        try {
//            Files.createDirectories(path);
//            LOGGER.info("Creating volume in {}",
//                    pathStr);
//            final FileVolumeState fileVolumeState = fileSystemVolumeStateDao.create();
//            final FileVolume fileVolume = new FileVolume();
//            fileVolume.setStatus(FileVolume.VolumeUseStatus.ACTIVE);
//            fileVolume.setPath(pathStr);
//            fileVolume.setVolumeState(fileVolumeState);
//            set an arbitrary default limit size of 250MB on each volume to prevent the
//            filesystem from running out of space, assuming they have 500MB free of course.
//            getDefaultVolumeLimit(path).ifPresent(fileVolume::setByteLimit);
//
//            create(fileVolume);
//
//
//            final FileVolume result = contextResultWithOptimisticLocking(connectionProvider, (context) -> {
//                AuditUtil.stamp(securityContext.getUserId(), fileVolume);
//                FsVolumeRecord record = context.newRecord(FS_VOLUME, fileVolume);
//                record.set(FS_VOLUME.STATUS, fileVolume.getStatus().getPrimitiveValue());
//                record.set(FS_VOLUME.FK_FS_VOLUME_STATE_ID, fileVolumeState.getId());
//                record.store();
//                return record.into(FileVolume.class);
//            });
//            result.setVolumeState(fileVolume.getVolumeState());
//        } catch (IOException e) {
//            LOGGER.error("Unable to create volume due to an error creating directory {}", pathStr, e);
//        }
    }

    private OptionalLong getDefaultVolumeLimit(final Path path) {
        try {
            long totalBytes = Files.getFileStore(path).getTotalSpace();
            // set an arbitrary limit of 90% of the filesystem total size to ensure we don't fill up the
            // filesystem.  Limit can be configured from within stroom.
            // Should be noted that although if you have multiple volumes on a filesystem the limit will apply
            // to all volumes and any other data on the filesystem. I.e. once the amount of the filesystem in use
            // is greater than the limit writes to those volumes will be prevented. See Volume.isFull() and
            // this.updateVolumeState()
            return OptionalLong.of((long) (totalBytes * volumeConfigProvider.get()
                    .getDefaultStreamVolumeFilesystemUtilisation()));
        } catch (IOException e) {
            LOGGER.warn(() -> LogUtil.message("Unable to determine the total space on the filesystem for path: {}",
                    FileUtil.getCanonicalPath(path)));
            return OptionalLong.empty();
        }
    }

    @Override
    public SystemInfoResult getSystemInfo() {

        final VolumeList volumeList = getCurrentVolumeList();

        // Need to wrap with optional as Map.ofEntries does not support null values.
        final var volInfoList = volumeList.getList()
                .stream()
                .map(vol -> Map.ofEntries(
                        new SimpleEntry<>("path", Optional.ofNullable(vol.getPath())),
                        new SimpleEntry<>("limit", Optional.ofNullable(vol.getByteLimit())),
                        new SimpleEntry<>("state", Optional.ofNullable(vol.getStatus())),
                        new SimpleEntry<>("free", Optional.ofNullable(NullSafe.get(
                                vol.getVolumeState(),
                                FsVolumeState::getBytesFree))),
                        new SimpleEntry<>("total", Optional.ofNullable(NullSafe.get(
                                vol.getVolumeState(),
                                FsVolumeState::getBytesTotal))),
                        new SimpleEntry<>("used", Optional.ofNullable(NullSafe.get(
                                vol.getVolumeState(),
                                FsVolumeState::getBytesUsed))),
                        new SimpleEntry<>("dbStateUpdateTime", Optional.ofNullable(NullSafe.get(
                                vol.getVolumeState(),
                                FsVolumeState::getUpdateTimeMs,
                                DateUtil::createNormalDateTimeString)))))
                .collect(Collectors.toList());

        return SystemInfoResult.builder(this)
                .addDetail("volumeSelector", volumeConfigProvider.get().getVolumeSelector())
                .addDetail("volumeListCreateTime", DateUtil.createNormalDateTimeString(volumeList.getCreateTime()))
                .addDetail("volumeList", volInfoList)
                .build();
    }

    private static class VolumeList {

        private final long createTime;
        private final List<FsVolume> list;

        VolumeList(final long createTime, final List<FsVolume> list) {
            this.createTime = createTime;
            this.list = list;
        }

        public List<FsVolume> getList() {
            return list;
        }

        public long getCreateTime() {
            return createTime;
        }
    }
}
