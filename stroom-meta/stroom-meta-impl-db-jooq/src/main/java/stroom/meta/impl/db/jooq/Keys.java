/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq;


import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;

import stroom.meta.impl.db.jooq.tables.Meta;
import stroom.meta.impl.db.jooq.tables.MetaFeed;
import stroom.meta.impl.db.jooq.tables.MetaKey;
import stroom.meta.impl.db.jooq.tables.MetaProcessor;
import stroom.meta.impl.db.jooq.tables.MetaRetentionTracker;
import stroom.meta.impl.db.jooq.tables.MetaType;
import stroom.meta.impl.db.jooq.tables.MetaVal;
import stroom.meta.impl.db.jooq.tables.records.MetaFeedRecord;
import stroom.meta.impl.db.jooq.tables.records.MetaKeyRecord;
import stroom.meta.impl.db.jooq.tables.records.MetaProcessorRecord;
import stroom.meta.impl.db.jooq.tables.records.MetaRecord;
import stroom.meta.impl.db.jooq.tables.records.MetaRetentionTrackerRecord;
import stroom.meta.impl.db.jooq.tables.records.MetaTypeRecord;
import stroom.meta.impl.db.jooq.tables.records.MetaValRecord;


/**
 * A class modelling foreign key relationships and constraints of tables of 
 * the <code>stroom</code> schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<MetaRecord, Long> IDENTITY_META = Identities0.IDENTITY_META;
    public static final Identity<MetaFeedRecord, Integer> IDENTITY_META_FEED = Identities0.IDENTITY_META_FEED;
    public static final Identity<MetaKeyRecord, Integer> IDENTITY_META_KEY = Identities0.IDENTITY_META_KEY;
    public static final Identity<MetaProcessorRecord, Integer> IDENTITY_META_PROCESSOR = Identities0.IDENTITY_META_PROCESSOR;
    public static final Identity<MetaTypeRecord, Integer> IDENTITY_META_TYPE = Identities0.IDENTITY_META_TYPE;
    public static final Identity<MetaValRecord, Long> IDENTITY_META_VAL = Identities0.IDENTITY_META_VAL;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<MetaRecord> KEY_META_PRIMARY = UniqueKeys0.KEY_META_PRIMARY;
    public static final UniqueKey<MetaFeedRecord> KEY_META_FEED_PRIMARY = UniqueKeys0.KEY_META_FEED_PRIMARY;
    public static final UniqueKey<MetaFeedRecord> KEY_META_FEED_NAME = UniqueKeys0.KEY_META_FEED_NAME;
    public static final UniqueKey<MetaKeyRecord> KEY_META_KEY_PRIMARY = UniqueKeys0.KEY_META_KEY_PRIMARY;
    public static final UniqueKey<MetaKeyRecord> KEY_META_KEY_META_KEY_NAME = UniqueKeys0.KEY_META_KEY_META_KEY_NAME;
    public static final UniqueKey<MetaProcessorRecord> KEY_META_PROCESSOR_PRIMARY = UniqueKeys0.KEY_META_PROCESSOR_PRIMARY;
    public static final UniqueKey<MetaProcessorRecord> KEY_META_PROCESSOR_PROCESSOR_UUID = UniqueKeys0.KEY_META_PROCESSOR_PROCESSOR_UUID;
    public static final UniqueKey<MetaRetentionTrackerRecord> KEY_META_RETENTION_TRACKER_PRIMARY = UniqueKeys0.KEY_META_RETENTION_TRACKER_PRIMARY;
    public static final UniqueKey<MetaTypeRecord> KEY_META_TYPE_PRIMARY = UniqueKeys0.KEY_META_TYPE_PRIMARY;
    public static final UniqueKey<MetaTypeRecord> KEY_META_TYPE_NAME = UniqueKeys0.KEY_META_TYPE_NAME;
    public static final UniqueKey<MetaValRecord> KEY_META_VAL_PRIMARY = UniqueKeys0.KEY_META_VAL_PRIMARY;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<MetaRecord, MetaFeedRecord> META_FEED_ID = ForeignKeys0.META_FEED_ID;
    public static final ForeignKey<MetaRecord, MetaTypeRecord> META_TYPE_ID = ForeignKeys0.META_TYPE_ID;
    public static final ForeignKey<MetaRecord, MetaProcessorRecord> META_PROCESSOR_ID = ForeignKeys0.META_PROCESSOR_ID;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 {
        public static Identity<MetaRecord, Long> IDENTITY_META = Internal.createIdentity(Meta.META, Meta.META.ID);
        public static Identity<MetaFeedRecord, Integer> IDENTITY_META_FEED = Internal.createIdentity(MetaFeed.META_FEED, MetaFeed.META_FEED.ID);
        public static Identity<MetaKeyRecord, Integer> IDENTITY_META_KEY = Internal.createIdentity(MetaKey.META_KEY, MetaKey.META_KEY.ID);
        public static Identity<MetaProcessorRecord, Integer> IDENTITY_META_PROCESSOR = Internal.createIdentity(MetaProcessor.META_PROCESSOR, MetaProcessor.META_PROCESSOR.ID);
        public static Identity<MetaTypeRecord, Integer> IDENTITY_META_TYPE = Internal.createIdentity(MetaType.META_TYPE, MetaType.META_TYPE.ID);
        public static Identity<MetaValRecord, Long> IDENTITY_META_VAL = Internal.createIdentity(MetaVal.META_VAL, MetaVal.META_VAL.ID);
    }

    private static class UniqueKeys0 {
        public static final UniqueKey<MetaRecord> KEY_META_PRIMARY = Internal.createUniqueKey(Meta.META, "KEY_meta_PRIMARY", Meta.META.ID);
        public static final UniqueKey<MetaFeedRecord> KEY_META_FEED_PRIMARY = Internal.createUniqueKey(MetaFeed.META_FEED, "KEY_meta_feed_PRIMARY", MetaFeed.META_FEED.ID);
        public static final UniqueKey<MetaFeedRecord> KEY_META_FEED_NAME = Internal.createUniqueKey(MetaFeed.META_FEED, "KEY_meta_feed_name", MetaFeed.META_FEED.NAME);
        public static final UniqueKey<MetaKeyRecord> KEY_META_KEY_PRIMARY = Internal.createUniqueKey(MetaKey.META_KEY, "KEY_meta_key_PRIMARY", MetaKey.META_KEY.ID);
        public static final UniqueKey<MetaKeyRecord> KEY_META_KEY_META_KEY_NAME = Internal.createUniqueKey(MetaKey.META_KEY, "KEY_meta_key_meta_key_name", MetaKey.META_KEY.NAME);
        public static final UniqueKey<MetaProcessorRecord> KEY_META_PROCESSOR_PRIMARY = Internal.createUniqueKey(MetaProcessor.META_PROCESSOR, "KEY_meta_processor_PRIMARY", MetaProcessor.META_PROCESSOR.ID);
        public static final UniqueKey<MetaProcessorRecord> KEY_META_PROCESSOR_PROCESSOR_UUID = Internal.createUniqueKey(MetaProcessor.META_PROCESSOR, "KEY_meta_processor_processor_uuid", MetaProcessor.META_PROCESSOR.PROCESSOR_UUID);
        public static final UniqueKey<MetaRetentionTrackerRecord> KEY_META_RETENTION_TRACKER_PRIMARY = Internal.createUniqueKey(MetaRetentionTracker.META_RETENTION_TRACKER, "KEY_meta_retention_tracker_PRIMARY", MetaRetentionTracker.META_RETENTION_TRACKER.RETENTION_RULES_VERSION, MetaRetentionTracker.META_RETENTION_TRACKER.RULE_AGE);
        public static final UniqueKey<MetaTypeRecord> KEY_META_TYPE_PRIMARY = Internal.createUniqueKey(MetaType.META_TYPE, "KEY_meta_type_PRIMARY", MetaType.META_TYPE.ID);
        public static final UniqueKey<MetaTypeRecord> KEY_META_TYPE_NAME = Internal.createUniqueKey(MetaType.META_TYPE, "KEY_meta_type_name", MetaType.META_TYPE.NAME);
        public static final UniqueKey<MetaValRecord> KEY_META_VAL_PRIMARY = Internal.createUniqueKey(MetaVal.META_VAL, "KEY_meta_val_PRIMARY", MetaVal.META_VAL.ID);
    }

    private static class ForeignKeys0 {
        public static final ForeignKey<MetaRecord, MetaFeedRecord> META_FEED_ID = Internal.createForeignKey(stroom.meta.impl.db.jooq.Keys.KEY_META_FEED_PRIMARY, Meta.META, "meta_feed_id", Meta.META.FEED_ID);
        public static final ForeignKey<MetaRecord, MetaTypeRecord> META_TYPE_ID = Internal.createForeignKey(stroom.meta.impl.db.jooq.Keys.KEY_META_TYPE_PRIMARY, Meta.META, "meta_type_id", Meta.META.TYPE_ID);
        public static final ForeignKey<MetaRecord, MetaProcessorRecord> META_PROCESSOR_ID = Internal.createForeignKey(stroom.meta.impl.db.jooq.Keys.KEY_META_PROCESSOR_PRIMARY, Meta.META, "meta_processor_id", Meta.META.PROCESSOR_ID);
    }
}
