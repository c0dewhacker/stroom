/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq.tables;


import stroom.meta.impl.db.jooq.Indexes;
import stroom.meta.impl.db.jooq.Keys;
import stroom.meta.impl.db.jooq.Stroom;
import stroom.meta.impl.db.jooq.tables.records.MetaValRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row5;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetaVal extends TableImpl<MetaValRecord> {

    private static final long serialVersionUID = -293545220;

    /**
     * The reference instance of <code>stroom.meta_val</code>
     */
    public static final MetaVal META_VAL = new MetaVal();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MetaValRecord> getRecordType() {
        return MetaValRecord.class;
    }

    /**
     * The column <code>stroom.meta_val.id</code>.
     */
    public final TableField<MetaValRecord, Long> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.meta_val.create_time</code>.
     */
    public final TableField<MetaValRecord, Long> CREATE_TIME = createField(DSL.name("create_time"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.meta_val.meta_id</code>.
     */
    public final TableField<MetaValRecord, Long> META_ID = createField(DSL.name("meta_id"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.meta_val.meta_key_id</code>.
     */
    public final TableField<MetaValRecord, Integer> META_KEY_ID = createField(DSL.name("meta_key_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.meta_val.val</code>.
     */
    public final TableField<MetaValRecord, Long> VAL = createField(DSL.name("val"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * Create a <code>stroom.meta_val</code> table reference
     */
    public MetaVal() {
        this(DSL.name("meta_val"), null);
    }

    /**
     * Create an aliased <code>stroom.meta_val</code> table reference
     */
    public MetaVal(String alias) {
        this(DSL.name(alias), META_VAL);
    }

    /**
     * Create an aliased <code>stroom.meta_val</code> table reference
     */
    public MetaVal(Name alias) {
        this(alias, META_VAL);
    }

    private MetaVal(Name alias, Table<MetaValRecord> aliased) {
        this(alias, aliased, null);
    }

    private MetaVal(Name alias, Table<MetaValRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> MetaVal(Table<O> child, ForeignKey<O, MetaValRecord> key) {
        super(child, key, META_VAL);
    }

    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.META_VAL_META_VAL_CREATE_TIME, Indexes.META_VAL_META_VAL_META_ID, Indexes.META_VAL_PRIMARY);
    }

    @Override
    public Identity<MetaValRecord, Long> getIdentity() {
        return Keys.IDENTITY_META_VAL;
    }

    @Override
    public UniqueKey<MetaValRecord> getPrimaryKey() {
        return Keys.KEY_META_VAL_PRIMARY;
    }

    @Override
    public List<UniqueKey<MetaValRecord>> getKeys() {
        return Arrays.<UniqueKey<MetaValRecord>>asList(Keys.KEY_META_VAL_PRIMARY);
    }

    @Override
    public MetaVal as(String alias) {
        return new MetaVal(DSL.name(alias), this);
    }

    @Override
    public MetaVal as(Name alias) {
        return new MetaVal(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaVal rename(String name) {
        return new MetaVal(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaVal rename(Name name) {
        return new MetaVal(name, null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<Long, Long, Long, Integer, Long> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}
