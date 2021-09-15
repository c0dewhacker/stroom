package stroom.data.store.impl.fs.db;

import stroom.data.store.impl.fs.FsVolumeStateDao;
import stroom.data.store.impl.fs.db.jooq.tables.records.FsVolumeStateRecord;
import stroom.data.store.impl.fs.shared.FsVolumeState;
import stroom.db.util.GenericDao;
import stroom.db.util.JooqUtil;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

import static stroom.data.store.impl.fs.db.jooq.tables.FsVolumeState.FS_VOLUME_STATE;

@Singleton
public class FsVolumeStateDaoImpl implements FsVolumeStateDao {

    private final FsDataStoreDbConnProvider fsDataStoreDbConnProvider;
    private final GenericDao<FsVolumeStateRecord, FsVolumeState, Integer> genericDao;

    @Inject
    FsVolumeStateDaoImpl(final FsDataStoreDbConnProvider fsDataStoreDbConnProvider) {
        this.fsDataStoreDbConnProvider = fsDataStoreDbConnProvider;
        genericDao = new GenericDao<>(FS_VOLUME_STATE,
                FS_VOLUME_STATE.ID,
                FsVolumeState.class,
                fsDataStoreDbConnProvider);
    }

    @Override
    public FsVolumeState create(final FsVolumeState volumeState) {
        return genericDao.create(volumeState);
    }

    @Override
    public FsVolumeState update(final FsVolumeState volumeState) {
        return JooqUtil.contextResult(fsDataStoreDbConnProvider, context -> {
            final FsVolumeStateRecord record = context.newRecord(FS_VOLUME_STATE);
            record.from(volumeState);
            record.update();
            return record.into(FsVolumeState.class);
        });
    }

    @Override
    public boolean delete(int id) {
        return genericDao.delete(id);
    }

    @Override
    public Optional<FsVolumeState> fetch(int id) {
        return genericDao.fetch(id);
    }
}
