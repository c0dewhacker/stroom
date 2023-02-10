/*
 * This file is generated by jOOQ.
 */
package stroom.docstore.fav.impl.db.jooq;


import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

import stroom.docstore.fav.impl.db.jooq.tables.DocFavourite;


/**
 * A class modelling indexes of tables in stroom.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index DOC_FAVOURITE_DOC_FAVOURITE_FK_DOC_TYPE_DOC_UUID = Internal.createIndex(DSL.name("doc_favourite_fk_doc_type_doc_uuid"), DocFavourite.DOC_FAVOURITE, new OrderField[] { DocFavourite.DOC_FAVOURITE.DOC_TYPE, DocFavourite.DOC_FAVOURITE.DOC_UUID }, false);
}
