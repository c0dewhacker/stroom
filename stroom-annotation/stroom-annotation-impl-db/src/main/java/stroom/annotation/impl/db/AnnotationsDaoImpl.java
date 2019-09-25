package stroom.annotation.impl.db;

import org.jooq.Record;
import org.springframework.stereotype.Component;
import stroom.annotation.impl.AnnotationsDao;
import stroom.annotation.impl.db.jooq.tables.records.AnnotationRecord;
import stroom.annotation.shared.Annotation;
import stroom.annotation.shared.AnnotationDetail;
import stroom.annotation.shared.AnnotationEntry;
import stroom.annotation.shared.AnnotationEntry.EntryType;
import stroom.annotation.shared.CreateEntryRequest;
import stroom.db.util.JooqUtil;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static stroom.annotation.impl.db.jooq.tables.Annotation.ANNOTATION;
import static stroom.annotation.impl.db.jooq.tables.AnnotationEntry.ANNOTATION_ENTRY;

@Component
class AnnotationsDaoImpl implements AnnotationsDao {
    //    private final stroom.explorer.impl.db.jooq.tables.ExplorerPath p = EXPLORER_PATH.as("p");
//    private final stroom.explorer.impl.db.jooq.tables.ExplorerPath p1 = EXPLORER_PATH.as("p1");
//    private final stroom.explorer.impl.db.jooq.tables.ExplorerPath p2 = EXPLORER_PATH.as("p2");
//    private final stroom.annotation.impl.db.jooq.tables.Annotation a = ANNOTATION.as("a");
//    private final stroom.annotation.impl.db.jooq.tables.AnnotationHistory sh = ANNOTATION_HISTORY.as("ah");

    private static final Function<Record, Annotation> RECORD_TO_ANNOTATION_MAPPER = record -> {
        final Annotation annotation = new Annotation();
        annotation.setId(record.get(ANNOTATION.ID));
        annotation.setVersion(record.get(ANNOTATION.VERSION));
        annotation.setCreateTime(record.get(ANNOTATION.CREATE_TIME_MS));
        annotation.setCreateUser(record.get(ANNOTATION.CREATE_USER));
        annotation.setUpdateTime(record.get(ANNOTATION.UPDATE_TIME_MS));
        annotation.setUpdateUser(record.get(ANNOTATION.UPDATE_USER));
        annotation.setMetaId(record.get(ANNOTATION.META_ID));
        annotation.setEventId(record.get(ANNOTATION.EVENT_ID));
        annotation.setTitle(record.get(ANNOTATION.TITLE));
        annotation.setStatus(record.get(ANNOTATION.STATUS));
        annotation.setAssignedTo(record.get(ANNOTATION.ASSIGNED_TO));
        return annotation;

//        final IndexVolumeGroup indexVolumeGroup = new IndexVolumeGroup();
//        indexVolumeGroup.setId(record.get(INDEX_VOLUME_GROUP.ID));
//        indexVolumeGroup.setVersion(record.get(INDEX_VOLUME_GROUP.VERSION));
//        indexVolumeGroup.setCreateTimeMs(record.get(INDEX_VOLUME_GROUP.CREATE_TIME_MS));
//        indexVolumeGroup.setCreateUser(record.get(INDEX_VOLUME_GROUP.CREATE_USER));
//        indexVolumeGroup.setUpdateTimeMs(record.get(INDEX_VOLUME_GROUP.UPDATE_TIME_MS));
//        indexVolumeGroup.setUpdateUser(record.get(INDEX_VOLUME_GROUP.UPDATE_USER));
//        indexVolumeGroup.setName(record.get(INDEX_VOLUME_GROUP.NAME));
//        return indexVolumeGroup;
    };

//    private static final BiFunction<Annotation, AnnotationRecord, AnnotationRecord> ANNOTATION_TO_RECORD_MAPPER = (annotation, record) -> {
//        record.from(annotation);
//        record.set(ANNOTATION.ID, annotation.getId());
//        record.set(ANNOTATION.VERSION, annotation.getVersion());
//        record.set(ANNOTATION.CREATE_TIME_MS, annotation.getCreateTime());
//        record.set(ANNOTATION.CREATE_USER, annotation.getCreateUser());
//        record.set(ANNOTATION.UPDATE_TIME_MS, annotation.getUpdateTime());
//        record.set(ANNOTATION.UPDATE_USER, annotation.getUpdateUser());
//        record.set(ANNOTATION.META_ID, annotation.getMetaId());
//        record.set(ANNOTATION.EVENT_ID, annotation.getEventId());
//        record.set(ANNOTATION.STATUS, annotation.getStatus());
//        record.set(ANNOTATION.ASSIGNED_TO, annotation.getAssignedTo());
//
//        return record;
//    };

    private static final Function<Record, AnnotationEntry> RECORD_TO_ANNOTATION_ENTRY_MAPPER = record -> {
        final AnnotationEntry entry = new AnnotationEntry();
        entry.setId(record.get(ANNOTATION_ENTRY.ID));
        entry.setVersion(record.get(ANNOTATION_ENTRY.VERSION));
        entry.setCreateTime(record.get(ANNOTATION_ENTRY.CREATE_TIME_MS));
        entry.setCreateUser(record.get(ANNOTATION_ENTRY.CREATE_USER));
        entry.setUpdateTime(record.get(ANNOTATION_ENTRY.UPDATE_TIME_MS));
        entry.setUpdateUser(record.get(ANNOTATION_ENTRY.UPDATE_USER));
        entry.setEntryType(EntryType.fromPrimitive(record.get(ANNOTATION_ENTRY.TYPE)));
        entry.setData(record.get(ANNOTATION_ENTRY.DATA));
        return entry;

//        final IndexVolumeGroup indexVolumeGroup = new IndexVolumeGroup();
//        indexVolumeGroup.setId(record.get(INDEX_VOLUME_GROUP.ID));
//        indexVolumeGroup.setVersion(record.get(INDEX_VOLUME_GROUP.VERSION));
//        indexVolumeGroup.setCreateTimeMs(record.get(INDEX_VOLUME_GROUP.CREATE_TIME_MS));
//        indexVolumeGroup.setCreateUser(record.get(INDEX_VOLUME_GROUP.CREATE_USER));
//        indexVolumeGroup.setUpdateTimeMs(record.get(INDEX_VOLUME_GROUP.UPDATE_TIME_MS));
//        indexVolumeGroup.setUpdateUser(record.get(INDEX_VOLUME_GROUP.UPDATE_USER));
//        indexVolumeGroup.setName(record.get(INDEX_VOLUME_GROUP.NAME));
//        return indexVolumeGroup;
    };

//    private void annotationToRecord(final Annotation annotation, final AnnotationRecord record) {
//        annotation.setId(record.getId());
//        annotation.setVersion(record.getVersion());
//        annotation.setCreateTime(record.getCreateTimeMs());
//        annotation.setCreateUser(record.getCreateUser());
//        annotation.setUpdateTime(record.getUpdateTimeMs());
//        annotation.setUpdateUser(record.getUpdateUser());
//        annotation.setMetaId(record.getMetaId());
//        annotation.setEventId(record.getEventId());
//        annotation.setStatus(record.getCurrentStatus());
//        annotation.setAssignedTo(record.getCurrentAssignee());
//    }
//
//    private Annotation recordToAnnotation(final AnnotationRecord record) {
//        final Annotation annotation = new Annotation();
//        record.setId(annotation.getId());
//        record.setVersion(annotation.getVersion());
//        record.setCreateTimeMs(annotation.getCreateTime());
//        record.setCreateUser(annotation.getCreateUser());
//        record.setUpdateTimeMs(annotation.getUpdateTime());
//        record.setUpdateUser(annotation.getUpdateUser());
//        record.setMetaId(annotation.getMetaId());
//        record.setEventId(annotation.getEventId());
//        record.setCurrentStatus(annotation.getStatus());
//        record.setCurrentAssignee(annotation.getAssignedTo());
//        return annotation;
//    }


    private final ConnectionProvider connectionProvider;


    @Inject
    AnnotationsDaoImpl(final ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

//    @Override
//    public Annotation get(final Long metaId, final Long eventId) {
////        return JooqUtil.contextResult(connectionProvider, context -> context
////                .selectFrom(ANNOTATION)
////                .where(ANNOTATION.META_ID.eq(metaId))
////                .and(ANNOTATION.EVENT_ID.eq(eventId))
////                .fetchOptional()
////                .map(this::recordToAnnotation)
////                .orElse(null));
//
//
//
//        Optional<Integer> optional = JooqUtil.contextResult(connectionProvider, context -> context
//                .insertInto(ANNOTATION,
//                        ANNOTATION.VERSION,
//                        ANNOTATION.CREATE_USER,
//                        ANNOTATION.CREATE_TIME_MS,
//                        ANNOTATION.UPDATE_USER,
//                        ANNOTATION.UPDATE_TIME_MS,
//                        ANNOTATION.META_ID,
//                        ANNOTATION.EVENT_ID,
//                        ANNOTATION.CURRENT_STATUS,
//                        ANNOTATION.CURRENT_ASSIGNEE)
//                .values(1,
//                        indexVolumeGroup.getCreateUser(),
//                        indexVolumeGroup.getCreateTimeMs(),
//                        indexVolumeGroup.getUpdateUser(),
//                        indexVolumeGroup.getUpdateTimeMs(),
//                        indexVolumeGroup.getName())
//                .onDuplicateKeyIgnore()
//                .returning(ANNOTATION.ID)
//                .fetchOptional()
//                .map(IndexVolumeGroupRecord::getId));
//
//        return optional.map(id -> {
//            indexVolumeGroup.setId(id);
//            indexVolumeGroup.setVersion(1);
//            return indexVolumeGroup;
//        }).orElse(get(indexVolumeGroup.getName()));
//    }

    @Override
    public Annotation get(final long metaId, final long eventId) {
        return JooqUtil.contextResult(connectionProvider, context -> context
                .select()
                .from(ANNOTATION)
                .where(ANNOTATION.META_ID.eq(metaId).and(ANNOTATION.EVENT_ID.eq(eventId)))
                .fetchOptional()
                .map(RECORD_TO_ANNOTATION_MAPPER)
                .orElse(null));
    }

    private Annotation get(final Annotation annotation) {
        Annotation result = get(annotation.getMetaId(), annotation.getEventId());
        if (result == null) {
            return annotation;
        }
        return result;
    }

    @Override
    public AnnotationDetail getDetail(final long metaId, final long eventId) {
        final Annotation annotation = get(metaId, eventId);
        if (annotation == null) {
            return null;
        }

        final List<AnnotationEntry> entries = JooqUtil.contextResult(connectionProvider, context -> context
                .select()
                .from(ANNOTATION_ENTRY)
                .where(ANNOTATION_ENTRY.FK_ANNOTATION_ID.eq(annotation.getId()))
                .orderBy(ANNOTATION_ENTRY.ID)
                .fetch()
                .map(RECORD_TO_ANNOTATION_ENTRY_MAPPER::apply));

        return new AnnotationDetail(annotation, entries);
    }

    @Override
    public AnnotationDetail createEntry(final CreateEntryRequest request, final String user) {
        final long now = System.currentTimeMillis();

        // Create the parent annotation first if it hasn't been already.
        Annotation parentAnnotation = get(request.getMetaId(), request.getEventId());
        if (parentAnnotation == null) {
            parentAnnotation = new Annotation();
            parentAnnotation.setMetaId(request.getMetaId());
            parentAnnotation.setEventId(request.getEventId());
            parentAnnotation.setTitle(request.getTitle());
            parentAnnotation.setCreateTime(now);
            parentAnnotation.setCreateUser(user);
            parentAnnotation.setUpdateTime(now);
            parentAnnotation.setUpdateUser(user);
            parentAnnotation.setStatus(request.getStatus());
            parentAnnotation.setAssignedTo(request.getAssignedTo());
            parentAnnotation = create(parentAnnotation);
        }

        // Update parent if we need to.
        final long annotationId = parentAnnotation.getId();
        if (EntryType.TITLE.equals(request.getEntryType())) {
            JooqUtil.context(connectionProvider, context -> context
                    .update(ANNOTATION)
                    .set(ANNOTATION.TITLE, request.getTitle())
                    .set(ANNOTATION.UPDATE_USER, user)
                    .set(ANNOTATION.UPDATE_TIME_MS, now)
                    .where(ANNOTATION.ID.eq(annotationId))
                    .execute());
        } else if (EntryType.STATUS.equals(request.getEntryType())) {
            JooqUtil.context(connectionProvider, context -> context
                    .update(ANNOTATION)
                    .set(ANNOTATION.STATUS, request.getStatus())
                    .set(ANNOTATION.UPDATE_USER, user)
                    .set(ANNOTATION.UPDATE_TIME_MS, now)
                    .where(ANNOTATION.ID.eq(annotationId))
                    .execute());
        } else if (EntryType.ASSIGNED_TO.equals(request.getEntryType())) {
            JooqUtil.context(connectionProvider, context -> context
                    .update(ANNOTATION)
                    .set(ANNOTATION.ASSIGNED_TO, request.getAssignedTo())
                    .set(ANNOTATION.UPDATE_USER, user)
                    .set(ANNOTATION.UPDATE_TIME_MS, now)
                    .where(ANNOTATION.ID.eq(annotationId))
                    .execute());
        } else {
            JooqUtil.context(connectionProvider, context -> context
                    .update(ANNOTATION)
                    .set(ANNOTATION.UPDATE_USER, user)
                    .set(ANNOTATION.UPDATE_TIME_MS, now)
                    .where(ANNOTATION.ID.eq(annotationId))
                    .execute());
        }

        // Create entry.
        final int count = JooqUtil.contextResult(connectionProvider, context -> context
                .insertInto(ANNOTATION_ENTRY,
                        ANNOTATION_ENTRY.VERSION,
                        ANNOTATION_ENTRY.CREATE_USER,
                        ANNOTATION_ENTRY.CREATE_TIME_MS,
                        ANNOTATION_ENTRY.UPDATE_USER,
                        ANNOTATION_ENTRY.UPDATE_TIME_MS,
                        ANNOTATION_ENTRY.FK_ANNOTATION_ID,
                        ANNOTATION_ENTRY.TYPE,
                        ANNOTATION_ENTRY.DATA)
                .values(1,
                        user,
                        now,
                        user,
                        now,
                        annotationId,
                        request.getEntryType().getPrimitiveValue(),
                        getData(request))
                .execute());

        if (count != 1) {
            throw new RuntimeException("Unable to create annotation entry");
        }

        // Now select everything back to provide refreshed details.
        return getDetail(request.getMetaId(), request.getEventId());
    }

    private String getData(final CreateEntryRequest request) {
        switch (request.getEntryType()) {
            case TITLE:
                return request.getTitle();
            case COMMENT:
                return request.getComment();
            case STATUS:
                return request.getStatus();
            case ASSIGNED_TO:
                return request.getAssignedTo();
        }
        return null;
    }

    private Annotation create(final Annotation annotation) {
        final Optional<Long> optional = JooqUtil.contextResult(connectionProvider, context -> context
                .insertInto(ANNOTATION,
                        ANNOTATION.VERSION,
                        ANNOTATION.CREATE_USER,
                        ANNOTATION.CREATE_TIME_MS,
                        ANNOTATION.UPDATE_USER,
                        ANNOTATION.UPDATE_TIME_MS,
                        ANNOTATION.META_ID,
                        ANNOTATION.EVENT_ID,
                        ANNOTATION.TITLE,
                        ANNOTATION.STATUS,
                        ANNOTATION.ASSIGNED_TO)
                .values(1,
                        annotation.getCreateUser(),
                        annotation.getCreateTime(),
                        annotation.getUpdateUser(),
                        annotation.getUpdateTime(),
                        annotation.getMetaId(),
                        annotation.getEventId(),
                        annotation.getTitle(),
                        annotation.getStatus(),
                        annotation.getAssignedTo())
                .onDuplicateKeyIgnore()
                .returning(ANNOTATION.ID)
                .fetchOptional()
                .map(AnnotationRecord::getId));

        return optional.map(id -> {
            annotation.setId(id);
            annotation.setVersion(1);
            return annotation;
        }).orElse(get(annotation));
    }
}
