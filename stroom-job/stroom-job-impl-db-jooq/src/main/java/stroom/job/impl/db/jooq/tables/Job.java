/*
 * This file is generated by jOOQ.
 */
package stroom.job.impl.db.jooq.tables;


import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import stroom.job.impl.db.jooq.Indexes;
import stroom.job.impl.db.jooq.Keys;
import stroom.job.impl.db.jooq.Stroom;
import stroom.job.impl.db.jooq.tables.records.JobRecord;

import javax.annotation.Generated;
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
public class Job extends TableImpl<JobRecord> {

    private static final long serialVersionUID = -1938575807;

    /**
     * The reference instance of <code>stroom.job</code>
     */
    public static final Job JOB = new Job();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<JobRecord> getRecordType() {
        return JobRecord.class;
    }

    /**
     * The column <code>stroom.job.id</code>.
     */
    public final TableField<JobRecord, Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.job.version</code>.
     */
    public final TableField<JobRecord, Integer> VERSION = createField("version", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.job.create_time_ms</code>.
     */
    public final TableField<JobRecord, Long> CREATE_TIME_MS = createField("create_time_ms", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.job.create_user</code>.
     */
    public final TableField<JobRecord, String> CREATE_USER = createField("create_user", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.job.update_time_ms</code>.
     */
    public final TableField<JobRecord, Long> UPDATE_TIME_MS = createField("update_time_ms", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.job.update_user</code>.
     */
    public final TableField<JobRecord, String> UPDATE_USER = createField("update_user", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.job.name</code>.
     */
    public final TableField<JobRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.job.enabled</code>.
     */
    public final TableField<JobRecord, Boolean> ENABLED = createField("enabled", org.jooq.impl.SQLDataType.BIT.nullable(false), this, "");

    /**
     * Create a <code>stroom.job</code> table reference
     */
    public Job() {
        this(DSL.name("job"), null);
    }

    /**
     * Create an aliased <code>stroom.job</code> table reference
     */
    public Job(String alias) {
        this(DSL.name(alias), JOB);
    }

    /**
     * Create an aliased <code>stroom.job</code> table reference
     */
    public Job(Name alias) {
        this(alias, JOB);
    }

    private Job(Name alias, Table<JobRecord> aliased) {
        this(alias, aliased, null);
    }

    private Job(Name alias, Table<JobRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> Job(Table<O> child, ForeignKey<O, JobRecord> key) {
        super(child, key, JOB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.JOB_NAME, Indexes.JOB_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<JobRecord, Integer> getIdentity() {
        return Keys.IDENTITY_JOB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<JobRecord> getPrimaryKey() {
        return Keys.KEY_JOB_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<JobRecord>> getKeys() {
        return Arrays.<UniqueKey<JobRecord>>asList(Keys.KEY_JOB_PRIMARY, Keys.KEY_JOB_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableField<JobRecord, Integer> getRecordVersion() {
        return VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Job as(String alias) {
        return new Job(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Job as(Name alias) {
        return new Job(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Job rename(String name) {
        return new Job(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Job rename(Name name) {
        return new Job(name, null);
    }
}
