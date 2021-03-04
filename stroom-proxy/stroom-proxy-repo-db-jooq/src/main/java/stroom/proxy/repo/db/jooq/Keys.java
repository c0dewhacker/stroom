/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq;


import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

import stroom.proxy.repo.db.jooq.tables.Aggregate;
import stroom.proxy.repo.db.jooq.tables.AggregateItem;
import stroom.proxy.repo.db.jooq.tables.ForwardAggregate;
import stroom.proxy.repo.db.jooq.tables.ForwardUrl;
import stroom.proxy.repo.db.jooq.tables.Source;
import stroom.proxy.repo.db.jooq.tables.SourceEntry;
import stroom.proxy.repo.db.jooq.tables.SourceItem;
import stroom.proxy.repo.db.jooq.tables.records.AggregateItemRecord;
import stroom.proxy.repo.db.jooq.tables.records.AggregateRecord;
import stroom.proxy.repo.db.jooq.tables.records.ForwardAggregateRecord;
import stroom.proxy.repo.db.jooq.tables.records.ForwardUrlRecord;
import stroom.proxy.repo.db.jooq.tables.records.SourceEntryRecord;
import stroom.proxy.repo.db.jooq.tables.records.SourceItemRecord;
import stroom.proxy.repo.db.jooq.tables.records.SourceRecord;


/**
 * A class modelling foreign key relationships and constraints of tables in 
 * the default schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<AggregateRecord> PK_AGGREGATE = Internal.createUniqueKey(Aggregate.AGGREGATE, DSL.name("pk_aggregate"), new TableField[] { Aggregate.AGGREGATE.ID }, true);
    public static final UniqueKey<AggregateItemRecord> PK_AGGREGATE_ITEM = Internal.createUniqueKey(AggregateItem.AGGREGATE_ITEM, DSL.name("pk_aggregate_item"), new TableField[] { AggregateItem.AGGREGATE_ITEM.ID }, true);
    public static final UniqueKey<ForwardAggregateRecord> PK_FORWARD_AGGREGATE = Internal.createUniqueKey(ForwardAggregate.FORWARD_AGGREGATE, DSL.name("pk_forward_aggregate"), new TableField[] { ForwardAggregate.FORWARD_AGGREGATE.ID }, true);
    public static final UniqueKey<ForwardUrlRecord> PK_FORWARD_URL = Internal.createUniqueKey(ForwardUrl.FORWARD_URL, DSL.name("pk_forward_url"), new TableField[] { ForwardUrl.FORWARD_URL.ID }, true);
    public static final UniqueKey<ForwardUrlRecord> SQLITE_AUTOINDEX_FORWARD_URL_1 = Internal.createUniqueKey(ForwardUrl.FORWARD_URL, DSL.name("sqlite_autoindex_forward_url_1"), new TableField[] { ForwardUrl.FORWARD_URL.URL }, true);
    public static final UniqueKey<SourceRecord> PK_SOURCE = Internal.createUniqueKey(Source.SOURCE, DSL.name("pk_source"), new TableField[] { Source.SOURCE.ID }, true);
    public static final UniqueKey<SourceRecord> SQLITE_AUTOINDEX_SOURCE_2 = Internal.createUniqueKey(Source.SOURCE, DSL.name("sqlite_autoindex_source_2"), new TableField[] { Source.SOURCE.PATH }, true);
    public static final UniqueKey<SourceEntryRecord> PK_SOURCE_ENTRY = Internal.createUniqueKey(SourceEntry.SOURCE_ENTRY, DSL.name("pk_source_entry"), new TableField[] { SourceEntry.SOURCE_ENTRY.ID }, true);
    public static final UniqueKey<SourceItemRecord> PK_SOURCE_ITEM = Internal.createUniqueKey(SourceItem.SOURCE_ITEM, DSL.name("pk_source_item"), new TableField[] { SourceItem.SOURCE_ITEM.ID }, true);
    public static final UniqueKey<SourceItemRecord> SQLITE_AUTOINDEX_SOURCE_ITEM_2 = Internal.createUniqueKey(SourceItem.SOURCE_ITEM, DSL.name("sqlite_autoindex_source_item_2"), new TableField[] { SourceItem.SOURCE_ITEM.NAME, SourceItem.SOURCE_ITEM.FK_SOURCE_ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<AggregateItemRecord, AggregateRecord> FK_AGGREGATE_ITEM_AGGREGATE_1 = Internal.createForeignKey(AggregateItem.AGGREGATE_ITEM, DSL.name("fk_aggregate_item_aggregate_1"), new TableField[] { AggregateItem.AGGREGATE_ITEM.FK_AGGREGATE_ID }, Keys.PK_AGGREGATE, new TableField[] { Aggregate.AGGREGATE.ID }, true);
    public static final ForeignKey<AggregateItemRecord, SourceItemRecord> FK_AGGREGATE_ITEM_SOURCE_ITEM_1 = Internal.createForeignKey(AggregateItem.AGGREGATE_ITEM, DSL.name("fk_aggregate_item_source_item_1"), new TableField[] { AggregateItem.AGGREGATE_ITEM.FK_SOURCE_ITEM_ID }, Keys.PK_SOURCE_ITEM, new TableField[] { SourceItem.SOURCE_ITEM.ID }, true);
    public static final ForeignKey<ForwardAggregateRecord, AggregateRecord> FK_FORWARD_AGGREGATE_AGGREGATE_1 = Internal.createForeignKey(ForwardAggregate.FORWARD_AGGREGATE, DSL.name("fk_forward_aggregate_aggregate_1"), new TableField[] { ForwardAggregate.FORWARD_AGGREGATE.FK_AGGREGATE_ID }, Keys.PK_AGGREGATE, new TableField[] { Aggregate.AGGREGATE.ID }, true);
    public static final ForeignKey<ForwardAggregateRecord, ForwardUrlRecord> FK_FORWARD_AGGREGATE_FORWARD_URL_1 = Internal.createForeignKey(ForwardAggregate.FORWARD_AGGREGATE, DSL.name("fk_forward_aggregate_forward_url_1"), new TableField[] { ForwardAggregate.FORWARD_AGGREGATE.FK_FORWARD_URL_ID }, Keys.PK_FORWARD_URL, new TableField[] { ForwardUrl.FORWARD_URL.ID }, true);
    public static final ForeignKey<SourceEntryRecord, SourceItemRecord> FK_SOURCE_ENTRY_SOURCE_ITEM_1 = Internal.createForeignKey(SourceEntry.SOURCE_ENTRY, DSL.name("fk_source_entry_source_item_1"), new TableField[] { SourceEntry.SOURCE_ENTRY.FK_SOURCE_ITEM_ID }, Keys.PK_SOURCE_ITEM, new TableField[] { SourceItem.SOURCE_ITEM.ID }, true);
    public static final ForeignKey<SourceItemRecord, SourceRecord> FK_SOURCE_ITEM_SOURCE_1 = Internal.createForeignKey(SourceItem.SOURCE_ITEM, DSL.name("fk_source_item_source_1"), new TableField[] { SourceItem.SOURCE_ITEM.FK_SOURCE_ID }, Keys.PK_SOURCE, new TableField[] { Source.SOURCE.ID }, true);
}
