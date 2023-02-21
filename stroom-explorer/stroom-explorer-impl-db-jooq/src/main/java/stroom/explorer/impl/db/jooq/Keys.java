/*
 * This file is generated by jOOQ.
 */
package stroom.explorer.impl.db.jooq;


import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

import stroom.explorer.impl.db.jooq.tables.ExplorerFavourite;
import stroom.explorer.impl.db.jooq.tables.ExplorerNode;
import stroom.explorer.impl.db.jooq.tables.ExplorerPath;
import stroom.explorer.impl.db.jooq.tables.records.ExplorerFavouriteRecord;
import stroom.explorer.impl.db.jooq.tables.records.ExplorerNodeRecord;
import stroom.explorer.impl.db.jooq.tables.records.ExplorerPathRecord;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * stroom.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<ExplorerFavouriteRecord> KEY_EXPLORER_FAVOURITE_EXPLORER_FAVOURITE_FK_EN_TYPE_EN_UUID_USER_UUID = Internal.createUniqueKey(ExplorerFavourite.EXPLORER_FAVOURITE, DSL.name("KEY_explorer_favourite_explorer_favourite_fk_en_type_en_uuid_user_uuid"), new TableField[] { ExplorerFavourite.EXPLORER_FAVOURITE.DOC_TYPE, ExplorerFavourite.EXPLORER_FAVOURITE.DOC_UUID, ExplorerFavourite.EXPLORER_FAVOURITE.USER_UUID }, true);
    public static final UniqueKey<ExplorerFavouriteRecord> KEY_EXPLORER_FAVOURITE_PRIMARY = Internal.createUniqueKey(ExplorerFavourite.EXPLORER_FAVOURITE, DSL.name("KEY_explorer_favourite_PRIMARY"), new TableField[] { ExplorerFavourite.EXPLORER_FAVOURITE.ID }, true);
    public static final UniqueKey<ExplorerNodeRecord> KEY_EXPLORER_NODE_EXPLORER_NODE_TYPE_UUID = Internal.createUniqueKey(ExplorerNode.EXPLORER_NODE, DSL.name("KEY_explorer_node_explorer_node_type_uuid"), new TableField[] { ExplorerNode.EXPLORER_NODE.TYPE, ExplorerNode.EXPLORER_NODE.UUID }, true);
    public static final UniqueKey<ExplorerNodeRecord> KEY_EXPLORER_NODE_PRIMARY = Internal.createUniqueKey(ExplorerNode.EXPLORER_NODE, DSL.name("KEY_explorer_node_PRIMARY"), new TableField[] { ExplorerNode.EXPLORER_NODE.ID }, true);
    public static final UniqueKey<ExplorerPathRecord> KEY_EXPLORER_PATH_PRIMARY = Internal.createUniqueKey(ExplorerPath.EXPLORER_PATH, DSL.name("KEY_explorer_path_PRIMARY"), new TableField[] { ExplorerPath.EXPLORER_PATH.ANCESTOR, ExplorerPath.EXPLORER_PATH.DESCENDANT }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<ExplorerFavouriteRecord, ExplorerNodeRecord> EXPLORER_FAVOURITE_FK_EN_TYPE_EN_UUID = Internal.createForeignKey(ExplorerFavourite.EXPLORER_FAVOURITE, DSL.name("explorer_favourite_fk_en_type_en_uuid"), new TableField[] { ExplorerFavourite.EXPLORER_FAVOURITE.DOC_TYPE, ExplorerFavourite.EXPLORER_FAVOURITE.DOC_UUID }, Keys.KEY_EXPLORER_NODE_EXPLORER_NODE_TYPE_UUID, new TableField[] { ExplorerNode.EXPLORER_NODE.TYPE, ExplorerNode.EXPLORER_NODE.UUID }, true);
}
