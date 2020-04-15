/*
 * This file is generated by jOOQ.
 */
package stroom.authentication.impl.db.jooq.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row12;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import stroom.authentication.impl.db.jooq.Indexes;
import stroom.authentication.impl.db.jooq.Keys;
import stroom.authentication.impl.db.jooq.Stroom;
import stroom.authentication.impl.db.jooq.tables.records.JsonWebKeyRecord;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class JsonWebKey extends TableImpl<JsonWebKeyRecord> {

    private static final long serialVersionUID = 539266762;

    /**
     * The reference instance of <code>stroom.json_web_key</code>
     */
    public static final JsonWebKey JSON_WEB_KEY = new JsonWebKey();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<JsonWebKeyRecord> getRecordType() {
        return JsonWebKeyRecord.class;
    }

    /**
     * The column <code>stroom.json_web_key.id</code>.
     */
    public final TableField<JsonWebKeyRecord, Integer> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.json_web_key.version</code>.
     */
    public final TableField<JsonWebKeyRecord, Integer> VERSION = createField(DSL.name("version"), org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.create_time_ms</code>.
     */
    public final TableField<JsonWebKeyRecord, Long> CREATE_TIME_MS = createField(DSL.name("create_time_ms"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.create_user</code>.
     */
    public final TableField<JsonWebKeyRecord, String> CREATE_USER = createField(DSL.name("create_user"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.update_time_ms</code>.
     */
    public final TableField<JsonWebKeyRecord, Long> UPDATE_TIME_MS = createField(DSL.name("update_time_ms"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.update_user</code>.
     */
    public final TableField<JsonWebKeyRecord, String> UPDATE_USER = createField(DSL.name("update_user"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.fk_token_type_id</code>.
     */
    public final TableField<JsonWebKeyRecord, Integer> FK_TOKEN_TYPE_ID = createField(DSL.name("fk_token_type_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.key_id</code>.
     */
    public final TableField<JsonWebKeyRecord, String> KEY_ID = createField(DSL.name("key_id"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.json</code>.
     */
    public final TableField<JsonWebKeyRecord, String> JSON = createField(DSL.name("json"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>stroom.json_web_key.expires_on_ms</code>.
     */
    public final TableField<JsonWebKeyRecord, Long> EXPIRES_ON_MS = createField(DSL.name("expires_on_ms"), org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>stroom.json_web_key.comments</code>.
     */
    public final TableField<JsonWebKeyRecord, String> COMMENTS = createField(DSL.name("comments"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>stroom.json_web_key.enabled</code>.
     */
    public final TableField<JsonWebKeyRecord, Boolean> ENABLED = createField(DSL.name("enabled"), org.jooq.impl.SQLDataType.BIT.nullable(false), this, "");

    /**
     * Create a <code>stroom.json_web_key</code> table reference
     */
    public JsonWebKey() {
        this(DSL.name("json_web_key"), null);
    }

    /**
     * Create an aliased <code>stroom.json_web_key</code> table reference
     */
    public JsonWebKey(String alias) {
        this(DSL.name(alias), JSON_WEB_KEY);
    }

    /**
     * Create an aliased <code>stroom.json_web_key</code> table reference
     */
    public JsonWebKey(Name alias) {
        this(alias, JSON_WEB_KEY);
    }

    private JsonWebKey(Name alias, Table<JsonWebKeyRecord> aliased) {
        this(alias, aliased, null);
    }

    private JsonWebKey(Name alias, Table<JsonWebKeyRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> JsonWebKey(Table<O> child, ForeignKey<O, JsonWebKeyRecord> key) {
        super(child, key, JSON_WEB_KEY);
    }

    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.JSON_WEB_KEY_JSON_WEB_KEY_FK_TOKEN_TYPE_ID, Indexes.JSON_WEB_KEY_PRIMARY);
    }

    @Override
    public Identity<JsonWebKeyRecord, Integer> getIdentity() {
        return Keys.IDENTITY_JSON_WEB_KEY;
    }

    @Override
    public UniqueKey<JsonWebKeyRecord> getPrimaryKey() {
        return Keys.KEY_JSON_WEB_KEY_PRIMARY;
    }

    @Override
    public List<UniqueKey<JsonWebKeyRecord>> getKeys() {
        return Arrays.<UniqueKey<JsonWebKeyRecord>>asList(Keys.KEY_JSON_WEB_KEY_PRIMARY);
    }

    @Override
    public List<ForeignKey<JsonWebKeyRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<JsonWebKeyRecord, ?>>asList(Keys.JSON_WEB_KEY_FK_TOKEN_TYPE_ID);
    }

    public TokenType tokenType() {
        return new TokenType(this, Keys.JSON_WEB_KEY_FK_TOKEN_TYPE_ID);
    }

    @Override
    public TableField<JsonWebKeyRecord, Integer> getRecordVersion() {
        return VERSION;
    }

    @Override
    public JsonWebKey as(String alias) {
        return new JsonWebKey(DSL.name(alias), this);
    }

    @Override
    public JsonWebKey as(Name alias) {
        return new JsonWebKey(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public JsonWebKey rename(String name) {
        return new JsonWebKey(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public JsonWebKey rename(Name name) {
        return new JsonWebKey(name, null);
    }

    // -------------------------------------------------------------------------
    // Row12 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row12<Integer, Integer, Long, String, Long, String, Integer, String, String, Long, String, Boolean> fieldsRow() {
        return (Row12) super.fieldsRow();
    }
}
