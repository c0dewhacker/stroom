package stroom.analytics.impl;

import stroom.datasource.api.v2.DateField;
import stroom.datasource.api.v2.QueryField;
import stroom.datasource.api.v2.TextField;
import stroom.docref.DocRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AnalyticFields {

    public static final String ANALYTICS_STORE_TYPE = "Analytics";
    public static final DocRef ANALYTICS_DOC_REF = DocRef.builder()
            .type(ANALYTICS_STORE_TYPE)
            .uuid(ANALYTICS_STORE_TYPE)
            .name(ANALYTICS_STORE_TYPE)
            .build();

    public static final String NAME = "Name";
    public static final String UUID = "UUID";
    public static final String TIME = "Time";
    public static final String VALUE = "Value";
    private static final List<QueryField> FIELDS = new ArrayList<>();
    private static final Map<String, QueryField> FIELD_MAP;

    // Times
    public static final DateField TIME_FIELD = new DateField(TIME);

    public static final TextField NAME_FIELD = new TextField(NAME);
    public static final TextField UUID_FIELD = new TextField(UUID);
    public static final TextField VALUE_FIELD = new TextField(VALUE);

    static {
        FIELDS.add(TIME_FIELD);
        FIELDS.add(NAME_FIELD);
        FIELDS.add(UUID_FIELD);
        FIELDS.add(VALUE_FIELD);

        FIELD_MAP = FIELDS.stream().collect(Collectors.toMap(QueryField::getName, Function.identity()));
    }

    public static List<QueryField> getFields() {
        return new ArrayList<>(FIELDS);
    }

    public static Map<String, QueryField> getFieldMap() {
        return FIELD_MAP;
    }
}
