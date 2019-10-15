package stroom.annotation.impl.db;

import org.jooq.Condition;
import org.jooq.Cursor;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import stroom.annotation.api.AnnotationDataSource;
import stroom.annotation.impl.AnnotationDao;
import stroom.annotation.impl.db.jooq.tables.records.AnnotationRecord;
import stroom.annotation.shared.Annotation;
import stroom.annotation.shared.AnnotationDetail;
import stroom.annotation.shared.AnnotationEntry;
import stroom.annotation.shared.CreateEntryRequest;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValLong;
import stroom.dashboard.expression.v1.ValNull;
import stroom.dashboard.expression.v1.ValString;
import stroom.datasource.api.v2.DataSourceField;
import stroom.db.util.ExpressionMapper;
import stroom.db.util.ExpressionMapperFactory;
import stroom.db.util.JooqUtil;
import stroom.db.util.ValueMapper;
import stroom.db.util.ValueMapper.Mapper;
import stroom.entity.shared.ExpressionCriteria;
import stroom.entity.shared.PageRequest;
import stroom.entity.shared.Sort;
import stroom.query.api.v2.ExpressionOperator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static stroom.annotation.impl.db.jooq.tables.Annotation.ANNOTATION;
import static stroom.annotation.impl.db.jooq.tables.AnnotationEntry.ANNOTATION_ENTRY;

@Component
class AnnotationDaoImpl implements AnnotationDao {
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
        annotation.setSubject(record.get(ANNOTATION.SUBJECT));
        annotation.setStatus(record.get(ANNOTATION.STATUS));
        annotation.setAssignedTo(record.get(ANNOTATION.ASSIGNED_TO));
        annotation.setComment(record.get(ANNOTATION.COMMENT));
        annotation.setHistory(record.get(ANNOTATION.HISTORY));
        return annotation;
    };

    private static final Function<Record, AnnotationEntry> RECORD_TO_ANNOTATION_ENTRY_MAPPER = record -> {
        final AnnotationEntry entry = new AnnotationEntry();
        entry.setId(record.get(ANNOTATION_ENTRY.ID));
        entry.setVersion(record.get(ANNOTATION_ENTRY.VERSION));
        entry.setCreateTime(record.get(ANNOTATION_ENTRY.CREATE_TIME_MS));
        entry.setCreateUser(record.get(ANNOTATION_ENTRY.CREATE_USER));
        entry.setUpdateTime(record.get(ANNOTATION_ENTRY.UPDATE_TIME_MS));
        entry.setUpdateUser(record.get(ANNOTATION_ENTRY.UPDATE_USER));
        entry.setEntryType(record.get(ANNOTATION_ENTRY.TYPE));
        entry.setData(record.get(ANNOTATION_ENTRY.DATA));
        return entry;
    };

    private static final Map<String, Field<String>> UPDATE_FIELD_MAP = new HashMap<>();

    static {
        UPDATE_FIELD_MAP.put(Annotation.TITLE, ANNOTATION.TITLE);
        UPDATE_FIELD_MAP.put(Annotation.SUBJECT, ANNOTATION.SUBJECT);
        UPDATE_FIELD_MAP.put(Annotation.STATUS, ANNOTATION.STATUS);
        UPDATE_FIELD_MAP.put(Annotation.ASSIGNED_TO, ANNOTATION.ASSIGNED_TO);
        UPDATE_FIELD_MAP.put(Annotation.COMMENT, ANNOTATION.COMMENT);
    }

    private final ConnectionProvider connectionProvider;
    private final ExpressionMapper expressionMapper;
    private final ValueMapper valueMapper;

    @Inject
    AnnotationDaoImpl(final ConnectionProvider connectionProvider,
                      final ExpressionMapperFactory expressionMapperFactory) {
        this.connectionProvider = connectionProvider;

        expressionMapper = expressionMapperFactory.create();
        expressionMapper.map(AnnotationDataSource.ID_FIELD, ANNOTATION.ID, Long::valueOf);
        expressionMapper.map(AnnotationDataSource.STREAM_ID_FIELD, ANNOTATION.META_ID, Long::valueOf);
        expressionMapper.map(AnnotationDataSource.EVENT_ID_FIELD, ANNOTATION.EVENT_ID, Long::valueOf);
        expressionMapper.map(AnnotationDataSource.CREATED_BY_FIELD, ANNOTATION.CREATE_USER, value -> value);
        expressionMapper.map(AnnotationDataSource.TITLE_FIELD, ANNOTATION.TITLE, value -> value);
        expressionMapper.map(AnnotationDataSource.SUBJECT_FIELD, ANNOTATION.SUBJECT, value -> value);
        expressionMapper.map(AnnotationDataSource.STATUS_FIELD, ANNOTATION.STATUS, value -> value);
        expressionMapper.map(AnnotationDataSource.ASSIGNED_TO_FIELD, ANNOTATION.ASSIGNED_TO, value -> value);
        expressionMapper.map(AnnotationDataSource.COMMENT_FIELD, ANNOTATION.COMMENT, value -> value);
        expressionMapper.map(AnnotationDataSource.HISTORY_FIELD, ANNOTATION.HISTORY, value -> value);

        valueMapper = new ValueMapper();
        valueMapper.map(AnnotationDataSource.ID_FIELD, ANNOTATION.ID, ValLong::create);
        valueMapper.map(AnnotationDataSource.STREAM_ID_FIELD, ANNOTATION.META_ID, ValLong::create);
        valueMapper.map(AnnotationDataSource.EVENT_ID_FIELD, ANNOTATION.EVENT_ID, ValLong::create);
        valueMapper.map(AnnotationDataSource.CREATED_BY_FIELD, ANNOTATION.CREATE_USER, ValString::create);
        valueMapper.map(AnnotationDataSource.TITLE_FIELD, ANNOTATION.TITLE, ValString::create);
        valueMapper.map(AnnotationDataSource.SUBJECT_FIELD, ANNOTATION.SUBJECT, ValString::create);
        valueMapper.map(AnnotationDataSource.STATUS_FIELD, ANNOTATION.STATUS, ValString::create);
        valueMapper.map(AnnotationDataSource.ASSIGNED_TO_FIELD, ANNOTATION.ASSIGNED_TO, ValString::create);
        valueMapper.map(AnnotationDataSource.COMMENT_FIELD, ANNOTATION.COMMENT, ValString::create);
        valueMapper.map(AnnotationDataSource.HISTORY_FIELD, ANNOTATION.HISTORY, ValString::create);
    }

    @Override
    public Annotation get(final long annotationId) {
        return JooqUtil.contextResult(connectionProvider, context -> context
                .select()
                .from(ANNOTATION)
                .where(ANNOTATION.ID.eq(annotationId))
                .fetchOptional()
                .map(RECORD_TO_ANNOTATION_MAPPER)
                .orElse(null));
    }

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
    public AnnotationDetail getDetail(final long annotationId) {
        final Annotation annotation = get(annotationId);
        if (annotation == null) {
            return null;
        }
        return getDetail(annotation);
    }

    @Override
    public AnnotationDetail getDetail(final long metaId, final long eventId) {
        final Annotation annotation = get(metaId, eventId);
        if (annotation == null) {
            return null;
        }
        return getDetail(annotation);
    }

    private AnnotationDetail getDetail(final Annotation annotation) {
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
        Annotation parentAnnotation = get(request.getAnnotation());
        if (parentAnnotation.getId() == null) {
            parentAnnotation = request.getAnnotation();
            parentAnnotation.setCreateTime(now);
            parentAnnotation.setCreateUser(user);
            parentAnnotation.setUpdateTime(now);
            parentAnnotation.setUpdateUser(user);
            parentAnnotation = create(parentAnnotation);

        } else {
            // Update parent if we need to.
            final long annotationId = parentAnnotation.getId();
            final Field<String> field = UPDATE_FIELD_MAP.get(request.getType());

            if (ANNOTATION.COMMENT.equals(field)) {
                JooqUtil.context(connectionProvider, context -> context
                        .update(ANNOTATION)
                        .set(ANNOTATION.COMMENT, request.getData())
                        .set(ANNOTATION.HISTORY, DSL.trim(DSL.concat(ANNOTATION.HISTORY, " " + request.getData())))
                        .set(ANNOTATION.UPDATE_USER, user)
                        .set(ANNOTATION.UPDATE_TIME_MS, now)
                        .where(ANNOTATION.ID.eq(annotationId))
                        .execute());

            } else if (field != null) {
                JooqUtil.context(connectionProvider, context -> context
                        .update(ANNOTATION)
                        .set(field, request.getData())
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
        }

        // Create entry.
        final long annotationId = parentAnnotation.getId();
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
                        request.getType(),
                        request.getData())
                .execute());

        if (count != 1) {
            throw new RuntimeException("Unable to create annotation entry");
        }

        // Now select everything back to provide refreshed details.
        return getDetail(annotationId);
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
                        ANNOTATION.SUBJECT,
                        ANNOTATION.STATUS,
                        ANNOTATION.ASSIGNED_TO,
                        ANNOTATION.COMMENT,
                        ANNOTATION.HISTORY)
                .values(1,
                        annotation.getCreateUser(),
                        annotation.getCreateTime(),
                        annotation.getUpdateUser(),
                        annotation.getUpdateTime(),
                        annotation.getMetaId(),
                        annotation.getEventId(),
                        annotation.getTitle(),
                        annotation.getSubject(),
                        annotation.getStatus(),
                        annotation.getAssignedTo(),
                        annotation.getComment(),
                        annotation.getHistory())
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

    @Override
    public void search(final ExpressionCriteria criteria, final DataSourceField[] fields, final Consumer<Val[]> consumer) {
        final List<DataSourceField> fieldList = Arrays.asList(fields);

        final PageRequest pageRequest = criteria.getPageRequest();
        final Condition condition = createCondition(criteria.getExpression());
        final OrderField[] orderFields = createOrderFields(criteria);
        final List<Field<?>> dbFields = new ArrayList<>(valueMapper.getFields(fieldList));
        final Mapper[] mappers = valueMapper.getMappers(fields);

        JooqUtil.context(connectionProvider, context -> {
            int offset = 0;
            int numberOfRows = 1000000;

            if (pageRequest != null) {
                offset = pageRequest.getOffset().intValue();
                numberOfRows = pageRequest.getLength();
            }

            SelectJoinStep<?> select = context.select(dbFields).from(ANNOTATION);
            try (final Cursor<?> cursor = select
                    .where(condition)
                    .orderBy(orderFields)
                    .limit(offset, numberOfRows)
                    .fetchLazy()) {

                while (cursor.hasNext()) {
                    final Result<?> result = cursor.fetchNext(1000);
                    result.forEach(r -> {
                        final Val[] arr = new Val[fields.length];
                        for (int i = 0; i < fields.length; i++) {
                            Val val = ValNull.INSTANCE;
                            final Mapper<?> mapper = mappers[i];
                            if (mapper != null) {
                                val = mapper.map(r);
                            }
                            arr[i] = val;
                        }
                        consumer.accept(arr);
                    });
                }
            }
        });
    }

    private Condition createCondition(final ExpressionOperator expression) {
        return expressionMapper.apply(expression);
    }

    private OrderField[] createOrderFields(final ExpressionCriteria criteria) {
        if (criteria.getSortList() == null || criteria.getSortList().size() == 0) {
            return new OrderField[]{ANNOTATION.ID};
        }

        return criteria.getSortList().stream().map(sort -> {
            Field field;
            if (AnnotationDataSource.CREATED_BY.equals(sort.getField())) {
                field = ANNOTATION.CREATE_USER;
            } else if (AnnotationDataSource.TITLE.equals(sort.getField())) {
                field = ANNOTATION.TITLE;
            } else if (AnnotationDataSource.SUBJECT.equals(sort.getField())) {
                field = ANNOTATION.SUBJECT;
            } else if (AnnotationDataSource.STATUS.equals(sort.getField())) {
                field = ANNOTATION.STATUS;
            } else if (AnnotationDataSource.ASSIGNED_TO.equals(sort.getField())) {
                field = ANNOTATION.ASSIGNED_TO;
            } else if (AnnotationDataSource.COMMENT.equals(sort.getField())) {
                field = ANNOTATION.COMMENT;
            } else if (AnnotationDataSource.HISTORY.equals(sort.getField())) {
                field = ANNOTATION.HISTORY;
            } else {
                field = ANNOTATION.ID;
            }

            OrderField orderField = field;
            if (Sort.Direction.DESCENDING.equals(sort.getDirection())) {
                orderField = field.desc();
            }

            return orderField;
        }).toArray(OrderField[]::new);
    }
}
