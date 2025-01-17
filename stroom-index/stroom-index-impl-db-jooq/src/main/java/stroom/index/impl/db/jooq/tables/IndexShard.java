/*
 * This file is generated by jOOQ.
 */

package stroom.index.impl.db.jooq.tables;


import stroom.index.impl.db.jooq.Indexes;
import stroom.index.impl.db.jooq.Keys;
import stroom.index.impl.db.jooq.Stroom;
import stroom.index.impl.db.jooq.tables.records.IndexShardRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row14;
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
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class IndexShard extends TableImpl<IndexShardRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.index_shard</code>
     */
    public static final IndexShard INDEX_SHARD = new IndexShard();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<IndexShardRecord> getRecordType() {
        return IndexShardRecord.class;
    }

    /**
     * The column <code>stroom.index_shard.id</code>.
     */
    public final TableField<IndexShardRecord, Long> ID = createField(DSL.name("id"),
            SQLDataType.BIGINT.nullable(false).identity(true),
            this,
            "");

    /**
     * The column <code>stroom.index_shard.node_name</code>.
     */
    public final TableField<IndexShardRecord, String> NODE_NAME = createField(DSL.name("node_name"),
            SQLDataType.VARCHAR(255).nullable(false),
            this,
            "");

    /**
     * The column <code>stroom.index_shard.fk_volume_id</code>.
     */
    public final TableField<IndexShardRecord, Integer> FK_VOLUME_ID = createField(DSL.name("fk_volume_id"),
            SQLDataType.INTEGER.nullable(false),
            this,
            "");

    /**
     * The column <code>stroom.index_shard.index_uuid</code>.
     */
    public final TableField<IndexShardRecord, String> INDEX_UUID = createField(DSL.name("index_uuid"),
            SQLDataType.VARCHAR(255).nullable(false),
            this,
            "");

    /**
     * The column <code>stroom.index_shard.commit_document_count</code>.
     */
    public final TableField<IndexShardRecord, Integer> COMMIT_DOCUMENT_COUNT = createField(DSL.name(
            "commit_document_count"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>stroom.index_shard.commit_duration_ms</code>.
     */
    public final TableField<IndexShardRecord, Long> COMMIT_DURATION_MS = createField(DSL.name("commit_duration_ms"),
            SQLDataType.BIGINT,
            this,
            "");

    /**
     * The column <code>stroom.index_shard.commit_ms</code>.
     */
    public final TableField<IndexShardRecord, Long> COMMIT_MS = createField(DSL.name("commit_ms"),
            SQLDataType.BIGINT,
            this,
            "");

    /**
     * The column <code>stroom.index_shard.document_count</code>.
     */
    public final TableField<IndexShardRecord, Integer> DOCUMENT_COUNT = createField(DSL.name("document_count"),
            SQLDataType.INTEGER.defaultValue(DSL.inline("0", SQLDataType.INTEGER)),
            this,
            "");

    /**
     * The column <code>stroom.index_shard.file_size</code>.
     */
    public final TableField<IndexShardRecord, Long> FILE_SIZE = createField(DSL.name("file_size"),
            SQLDataType.BIGINT.defaultValue(DSL.inline("0", SQLDataType.BIGINT)),
            this,
            "");

    /**
     * The column <code>stroom.index_shard.status</code>.
     */
    public final TableField<IndexShardRecord, Byte> STATUS = createField(DSL.name("status"),
            SQLDataType.TINYINT.nullable(false),
            this,
            "");

    /**
     * The column <code>stroom.index_shard.index_version</code>.
     */
    public final TableField<IndexShardRecord, String> INDEX_VERSION = createField(DSL.name("index_version"),
            SQLDataType.VARCHAR(255),
            this,
            "");

    /**
     * The column <code>stroom.index_shard.partition_name</code>.
     */
    public final TableField<IndexShardRecord, String> PARTITION_NAME = createField(DSL.name("partition_name"),
            SQLDataType.VARCHAR(255).nullable(false),
            this,
            "");

    /**
     * The column <code>stroom.index_shard.partition_from_ms</code>.
     */
    public final TableField<IndexShardRecord, Long> PARTITION_FROM_MS = createField(DSL.name("partition_from_ms"),
            SQLDataType.BIGINT,
            this,
            "");

    /**
     * The column <code>stroom.index_shard.partition_to_ms</code>.
     */
    public final TableField<IndexShardRecord, Long> PARTITION_TO_MS = createField(DSL.name("partition_to_ms"),
            SQLDataType.BIGINT,
            this,
            "");

    private IndexShard(Name alias, Table<IndexShardRecord> aliased) {
        this(alias, aliased, null);
    }

    private IndexShard(Name alias, Table<IndexShardRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.index_shard</code> table reference
     */
    public IndexShard(String alias) {
        this(DSL.name(alias), INDEX_SHARD);
    }

    /**
     * Create an aliased <code>stroom.index_shard</code> table reference
     */
    public IndexShard(Name alias) {
        this(alias, INDEX_SHARD);
    }

    /**
     * Create a <code>stroom.index_shard</code> table reference
     */
    public IndexShard() {
        this(DSL.name("index_shard"), null);
    }

    public <O extends Record> IndexShard(Table<O> child, ForeignKey<O, IndexShardRecord> key) {
        super(child, key, INDEX_SHARD);
    }

    @Override
    public Schema getSchema() {
        return aliased()
                ? null
                : Stroom.STROOM;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.INDEX_SHARD_INDEX_SHARD_INDEX_UUID);
    }

    @Override
    public Identity<IndexShardRecord, Long> getIdentity() {
        return (Identity<IndexShardRecord, Long>) super.getIdentity();
    }

    @Override
    public UniqueKey<IndexShardRecord> getPrimaryKey() {
        return Keys.KEY_INDEX_SHARD_PRIMARY;
    }

    @Override
    public List<ForeignKey<IndexShardRecord, ?>> getReferences() {
        return Arrays.asList(Keys.INDEX_SHARD_FK_VOLUME_ID);
    }

    private transient IndexVolume _indexVolume;

    /**
     * Get the implicit join path to the <code>stroom.index_volume</code> table.
     */
    public IndexVolume indexVolume() {
        if (_indexVolume == null) {
            _indexVolume = new IndexVolume(this, Keys.INDEX_SHARD_FK_VOLUME_ID);
        }

        return _indexVolume;
    }

    @Override
    public IndexShard as(String alias) {
        return new IndexShard(DSL.name(alias), this);
    }

    @Override
    public IndexShard as(Name alias) {
        return new IndexShard(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public IndexShard rename(String name) {
        return new IndexShard(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public IndexShard rename(Name name) {
        return new IndexShard(name, null);
    }

    // -------------------------------------------------------------------------
    // Row14 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row14<Long, String, Integer, String, Integer, Long, Long, Integer, Long, Byte, String, String, Long, Long> fieldsRow() {
        return (Row14) super.fieldsRow();
    }
}
