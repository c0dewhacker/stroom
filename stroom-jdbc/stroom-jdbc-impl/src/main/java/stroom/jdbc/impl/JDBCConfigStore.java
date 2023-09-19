
package stroom.jdbc.impl;

import stroom.docref.DocRef;
import stroom.docstore.api.DocumentStore;
import stroom.jdbc.shared.JDBCConfigDoc;

import java.util.List;

public interface JDBCConfigStore extends DocumentStore<JDBCConfigDoc> {

    List<DocRef> list();
}
