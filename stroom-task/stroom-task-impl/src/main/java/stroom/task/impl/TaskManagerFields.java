package stroom.task.impl;

import stroom.datasource.api.v2.DateField;
import stroom.datasource.api.v2.LongField;
import stroom.datasource.api.v2.QueryField;
import stroom.datasource.api.v2.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaskManagerFields {

    public static final String FIELD_NODE = "Node";
    public static final String FIELD_NAME = "Name";
    public static final String FIELD_USER = "User";
    public static final String FIELD_SUBMIT_TIME = "Submit Time";
    public static final String FIELD_AGE = "Age";
    public static final String FIELD_INFO = "Info";

    private static final List<QueryField> FIELDS = new ArrayList<>();
    private static final Map<String, QueryField> FIELD_MAP;

    public static final TextField NODE = new TextField(FIELD_NODE);
    public static final TextField NAME = new TextField(FIELD_NAME);
    public static final TextField USER = new TextField(FIELD_USER);
    public static final DateField SUBMIT_TIME = new DateField(FIELD_SUBMIT_TIME);
    public static final LongField AGE = new LongField(FIELD_AGE);
    public static final TextField INFO = new TextField(FIELD_INFO);

    static {
        FIELDS.add(NODE);
        FIELDS.add(NAME);
        FIELDS.add(USER);
        FIELDS.add(SUBMIT_TIME);
        FIELDS.add(AGE);
        FIELDS.add(INFO);

        FIELD_MAP = FIELDS.stream().collect(Collectors.toMap(QueryField::getName, Function.identity()));
    }

    public static List<QueryField> getFields() {
        return new ArrayList<>(FIELDS);
    }

    public static Map<String, QueryField> getFieldMap() {
        return FIELD_MAP;
    }
}
