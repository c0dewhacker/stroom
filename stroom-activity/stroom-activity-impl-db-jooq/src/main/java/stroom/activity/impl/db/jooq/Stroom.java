/*
 * This file is generated by jOOQ.
 */
package stroom.activity.impl.db.jooq;


import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;
import stroom.activity.impl.db.jooq.tables.Activity;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Stroom extends SchemaImpl {

    private static final long serialVersionUID = 1631516918;

    /**
     * The reference instance of <code>stroom</code>
     */
    public static final Stroom STROOM = new Stroom();

    /**
     * The table <code>stroom.activity</code>.
     */
    public final Activity ACTIVITY = stroom.activity.impl.db.jooq.tables.Activity.ACTIVITY;

    /**
     * No further instances allowed
     */
    private Stroom() {
        super("stroom", null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        List result = new ArrayList();
        result.addAll(getTables0());
        return result;
    }

    private final List<Table<?>> getTables0() {
        return Arrays.<Table<?>>asList(
            Activity.ACTIVITY);
    }
}
