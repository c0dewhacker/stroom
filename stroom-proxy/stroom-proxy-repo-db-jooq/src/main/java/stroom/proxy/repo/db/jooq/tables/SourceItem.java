/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row7;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import stroom.proxy.repo.db.jooq.DefaultSchema;
import stroom.proxy.repo.db.jooq.Keys;
import stroom.proxy.repo.db.jooq.tables.records.SourceItemRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SourceItem extends TableImpl<SourceItemRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>source_item</code>
     */
    public static final SourceItem SOURCE_ITEM = new SourceItem();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SourceItemRecord> getRecordType() {
        return SourceItemRecord.class;
    }

    /**
     * The column <code>source_item.id</code>.
     */
    public final TableField<SourceItemRecord, Long> ID = createField(DSL.name("id"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>source_item.number</code>.
     */
    public final TableField<SourceItemRecord, Integer> NUMBER = createField(DSL.name("number"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>source_item.name</code>.
     */
    public final TableField<SourceItemRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>source_item.feed_name</code>.
     */
    public final TableField<SourceItemRecord, String> FEED_NAME = createField(DSL.name("feed_name"), SQLDataType.VARCHAR(255).defaultValue(DSL.field("NULL", SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>source_item.type_name</code>.
     */
    public final TableField<SourceItemRecord, String> TYPE_NAME = createField(DSL.name("type_name"), SQLDataType.VARCHAR(255).defaultValue(DSL.field("NULL", SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>source_item.fk_source_id</code>.
     */
    public final TableField<SourceItemRecord, Long> FK_SOURCE_ID = createField(DSL.name("fk_source_id"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>source_item.aggregated</code>.
     */
    public final TableField<SourceItemRecord, Boolean> AGGREGATED = createField(DSL.name("aggregated"), SQLDataType.BOOLEAN.defaultValue(DSL.field("FALSE", SQLDataType.BOOLEAN)), this, "");

    private SourceItem(Name alias, Table<SourceItemRecord> aliased) {
        this(alias, aliased, null);
    }

    private SourceItem(Name alias, Table<SourceItemRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>source_item</code> table reference
     */
    public SourceItem(String alias) {
        this(DSL.name(alias), SOURCE_ITEM);
    }

    /**
     * Create an aliased <code>source_item</code> table reference
     */
    public SourceItem(Name alias) {
        this(alias, SOURCE_ITEM);
    }

    /**
     * Create a <code>source_item</code> table reference
     */
    public SourceItem() {
        this(DSL.name("source_item"), null);
    }

    public <O extends Record> SourceItem(Table<O> child, ForeignKey<O, SourceItemRecord> key) {
        super(child, key, SOURCE_ITEM);
    }

    @Override
    public Schema getSchema() {
        return DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public UniqueKey<SourceItemRecord> getPrimaryKey() {
        return Keys.PK_SOURCE_ITEM;
    }

    @Override
    public List<UniqueKey<SourceItemRecord>> getKeys() {
        return Arrays.<UniqueKey<SourceItemRecord>>asList(Keys.PK_SOURCE_ITEM, Keys.SQLITE_AUTOINDEX_SOURCE_ITEM_2);
    }

    @Override
    public List<ForeignKey<SourceItemRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<SourceItemRecord, ?>>asList(Keys.FK_SOURCE_ITEM_SOURCE_1);
    }

    private transient Source _source;

    public Source source() {
        if (_source == null)
            _source = new Source(this, Keys.FK_SOURCE_ITEM_SOURCE_1);

        return _source;
    }

    @Override
    public SourceItem as(String alias) {
        return new SourceItem(DSL.name(alias), this);
    }

    @Override
    public SourceItem as(Name alias) {
        return new SourceItem(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public SourceItem rename(String name) {
        return new SourceItem(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public SourceItem rename(Name name) {
        return new SourceItem(name, null);
    }

    // -------------------------------------------------------------------------
    // Row7 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row7<Long, Integer, String, String, String, Long, Boolean> fieldsRow() {
        return (Row7) super.fieldsRow();
    }
}
