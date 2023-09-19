package stroom.jdbc.impl;

import stroom.docref.DocRef;
import stroom.docstore.api.DocumentResourceHelper;
import stroom.event.logging.rs.api.AutoLogged;
import stroom.jdbc.shared.JDBCConfigDoc;
import stroom.jdbc.shared.JDBCConfigResource;
import stroom.resource.api.ResourceStore;
import stroom.util.io.StreamUtil;
import stroom.util.shared.EntityServiceException;
import stroom.util.shared.FetchWithUuid;
import stroom.util.shared.ResourceGeneration;
import stroom.util.shared.ResourceKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.inject.Provider;

@AutoLogged
public class JDBCConfigResourceImpl implements JDBCConfigResource, FetchWithUuid<JDBCConfigDoc> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JDBCConfigResourceImpl.class);

    private final Provider<JDBCConfigStore> jdbcConfigStoreProvider;
    private final Provider<DocumentResourceHelper> documentResourceHelperProvider;
    private final Provider<ResourceStore> resourceStoreProvider;

    @Inject
    JDBCConfigResourceImpl(final Provider<JDBCConfigStore> jdbcConfigStoreProvider,
                            final Provider<DocumentResourceHelper> documentResourceHelperProvider,
                            final Provider<ResourceStore> resourceStoreProvider) {
        this.jdbcConfigStoreProvider = jdbcConfigStoreProvider;
        this.documentResourceHelperProvider = documentResourceHelperProvider;
        this.resourceStoreProvider = resourceStoreProvider;
    }

    @Override
    public JDBCConfigDoc fetch(final String uuid) {
        return documentResourceHelperProvider.get().read(jdbcConfigStoreProvider.get(), getDocRef(uuid));
    }

    @Override
    public JDBCConfigDoc update(final String uuid, final JDBCConfigDoc doc) {
        if (doc.getUuid() == null || !doc.getUuid().equals(uuid)) {
            throw new EntityServiceException("The document UUID must match the update UUID");
        }
        return documentResourceHelperProvider.get().update(jdbcConfigStoreProvider.get(), doc);
    }

    private DocRef getDocRef(final String uuid) {
        return DocRef.builder()
                .uuid(uuid)
                .type(JDBCConfigDoc.DOCUMENT_TYPE)
                .build();
    }

    @Override
    public ResourceGeneration download(final DocRef jdbcConfigDocRef) {
        final JDBCConfigDoc jdbcConfigDoc = jdbcConfigStoreProvider.get().readDocument(jdbcConfigDocRef);
        if (jdbcConfigDoc == null) {
            throw new EntityServiceException("Unable to find JDBC database config");
        }

        final ResourceKey resourceKey = resourceStoreProvider.get().createTempFile("jdbcConfig.properties");
        final Path file = resourceStoreProvider.get().getTempFile(resourceKey);
        try {
            Files.writeString(file, jdbcConfigDoc.getData(), StreamUtil.DEFAULT_CHARSET);
        } catch (IOException e) {
            LOGGER.error("Unable to download jdbcConfig", e);
            throw new UncheckedIOException(e);
        }

        return new ResourceGeneration(resourceKey, new ArrayList<>());
    }
}
