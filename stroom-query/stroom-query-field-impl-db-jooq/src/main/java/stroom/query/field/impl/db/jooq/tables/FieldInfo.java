/*
 * This file is generated by jOOQ.
 */
package stroom.query.field.impl.db.jooq.tables;


import stroom.query.field.impl.db.jooq.Keys;
import stroom.query.field.impl.db.jooq.Stroom;
import stroom.query.field.impl.db.jooq.tables.records.FieldInfoRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class FieldInfo extends TableImpl<FieldInfoRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.field_info</code>
     */
    public static final FieldInfo FIELD_INFO = new FieldInfo();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<FieldInfoRecord> getRecordType() {
        return FieldInfoRecord.class;
    }

    /**
     * The column <code>stroom.field_info.id</code>.
     */
    public final TableField<FieldInfoRecord, Long> ID = createField(DSL.name("id"), SQLDataType.BIGINT.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.field_info.fk_field_source_id</code>.
     */
    public final TableField<FieldInfoRecord, Integer> FK_FIELD_SOURCE_ID = createField(DSL.name("fk_field_source_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.field_info.field_type</code>.
     */
    public final TableField<FieldInfoRecord, Byte> FIELD_TYPE = createField(DSL.name("field_type"), SQLDataType.TINYINT.nullable(false), this, "");

    /**
     * The column <code>stroom.field_info.field_name</code>.
     */
    public final TableField<FieldInfoRecord, String> FIELD_NAME = createField(DSL.name("field_name"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    private FieldInfo(Name alias, Table<FieldInfoRecord> aliased) {
        this(alias, aliased, null);
    }

    private FieldInfo(Name alias, Table<FieldInfoRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.field_info</code> table reference
     */
    public FieldInfo(String alias) {
        this(DSL.name(alias), FIELD_INFO);
    }

    /**
     * Create an aliased <code>stroom.field_info</code> table reference
     */
    public FieldInfo(Name alias) {
        this(alias, FIELD_INFO);
    }

    /**
     * Create a <code>stroom.field_info</code> table reference
     */
    public FieldInfo() {
        this(DSL.name("field_info"), null);
    }

    public <O extends Record> FieldInfo(Table<O> child, ForeignKey<O, FieldInfoRecord> key) {
        super(child, key, FIELD_INFO);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public Identity<FieldInfoRecord, Long> getIdentity() {
        return (Identity<FieldInfoRecord, Long>) super.getIdentity();
    }

    @Override
    public UniqueKey<FieldInfoRecord> getPrimaryKey() {
        return Keys.KEY_FIELD_INFO_PRIMARY;
    }

    @Override
    public List<UniqueKey<FieldInfoRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_FIELD_INFO_FIELD_SOURCE_ID_FIELD_TYPE_FIELD_NAME);
    }

    @Override
    public List<ForeignKey<FieldInfoRecord, ?>> getReferences() {
        return Arrays.asList(Keys.FIELD_FK_FIELD_SOURCE_ID);
    }

    private transient FieldSource _fieldSource;

    /**
     * Get the implicit join path to the <code>stroom.field_source</code> table.
     */
    public FieldSource fieldSource() {
        if (_fieldSource == null)
            _fieldSource = new FieldSource(this, Keys.FIELD_FK_FIELD_SOURCE_ID);

        return _fieldSource;
    }

    @Override
    public FieldInfo as(String alias) {
        return new FieldInfo(DSL.name(alias), this);
    }

    @Override
    public FieldInfo as(Name alias) {
        return new FieldInfo(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public FieldInfo rename(String name) {
        return new FieldInfo(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public FieldInfo rename(Name name) {
        return new FieldInfo(name, null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<Long, Integer, Byte, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }
}
