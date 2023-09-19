package stroom.jdbc.pipeline;

import stroom.docref.DocRef;
import stroom.docstore.shared.DocRefUtil;
import stroom.jdbc.impl.JDBCConfigDocCache;
import stroom.jdbc.shared.JDBCConfigDoc;
import stroom.pipeline.LocationFactoryProxy;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.errorhandler.LoggedException;
import stroom.pipeline.factory.ConfigurableElement;
import stroom.pipeline.factory.PipelineProperty;
import stroom.pipeline.factory.PipelinePropertyDocRef;
import stroom.pipeline.filter.AbstractXMLFilter;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.svg.shared.SvgImage;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.Severity;

import org.xml.sax.Locator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.xml.transform.sax.TransformerHandler;


@ConfigurableElement(
        type = "JDBCDatabaseFilter",
        category = PipelineElementType.Category.DESTINATION,
        roles = {PipelineElementType.ROLE_TARGET,
                PipelineElementType.ROLE_HAS_TARGETS,
                PipelineElementType.VISABILITY_SIMPLE},
        icon = SvgImage.PIPELINE_STATISTICS)
public class JDBCDatabaseFilter extends AbstractXMLFilter {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(JDBCDatabaseFilter.class);

    private final ErrorReceiverProxy errorReceiverProxy;
    private final LocationFactoryProxy locationFactory;

    private final JDBCConfigDocCache jdbcConfigDocCache;

    private Locator locator = null;
    private DocRef configRef = null;

    private TransformerHandler xmlValueHandler;
    private ByteArrayOutputStream outputStream;
    private int xmlValueDepth = -1;
    private boolean flushOnSend = true;

    @Inject
    JDBCDatabaseFilter(final ErrorReceiverProxy errorReceiverProxy,
                       final LocationFactoryProxy locationFactory,
                       final JDBCConfigDocCache jdbcConfigDocCache) {
        this.errorReceiverProxy = errorReceiverProxy;
        this.locationFactory = locationFactory;
        this.jdbcConfigDocCache = jdbcConfigDocCache;
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator;
        super.setDocumentLocator(locator);
    }

    public static Properties getProperties(JDBCConfigDoc doc) {
        Properties properties = new Properties();
        if (doc.getData() != null && !doc.getData().isEmpty()) {
            StringReader reader = new StringReader(doc.getData());
            try {
                properties.load(reader);
            } catch (IOException ex) {
                LOGGER.error("Unable to read JDBC properties from {} - {}", doc.getName(), doc.getUuid(), ex);
            }
        }
        return properties;
    }

    public void getConnection(final DocRef jdbcConfigDocRef) {
        Objects.requireNonNull(jdbcConfigDocRef);
        Objects.requireNonNull(jdbcConfigDocRef.getUuid(),
                "No JDBC config UUID has been defined");

        final Optional<JDBCConfigDoc> optJDBCConfigDoc = jdbcConfigDocCache.get(jdbcConfigDocRef);

        if (optJDBCConfigDoc.isPresent()) {
            final JDBCConfigDoc jdbcConfigDoc = optJDBCConfigDoc.get();
            final DocRef docRefFromDoc = DocRefUtil.create(jdbcConfigDoc);
        }
    }

    @Override
    public void startProcessing() {
        try {
            if (configRef == null) {
                log(Severity.FATAL_ERROR, "JDBCConfig has not been set", null);
                throw LoggedException.create("JDBCConfig has not been set");
            }

            // Acquire a JDBC connection from the pool
            try {
                // Your JDBC logic to insert data into the database goes here
                // Use the 'connection' object to interact with the database
                System.out.println("Hello World");
            } catch (Exception e) {
                log(Severity.FATAL_ERROR, "Error executing JDBC insert", e);
            }
        } catch (Exception ex) {
            log(Severity.FATAL_ERROR, "Unable to create JDBC Producer using config " + configRef.getUuid(), ex);
        } finally {
            super.startProcessing();
        }
    }

    @Override
    public void endProcessing() {

        try {
            System.out.println("Hello World");
        } catch (Exception e) {
            log(Severity.FATAL_ERROR, "Error closing JDBC connection pool", e);
        }
        super.endProcessing();
    }

    // Other methods for handling XML parsing and JDBC insertion go here

    @PipelineProperty(
            description = "JDBC configuration details relating to where and how to send JDBC data.",
            displayPriority = 1)
    @PipelinePropertyDocRef(types = JDBCConfigDoc.DOCUMENT_TYPE)
    public void setJDBCConfig(final DocRef configRef) {
        this.configRef = configRef;
    }

    @SuppressWarnings("unused")
    @PipelineProperty(
            description = "At the end of the stream, wait for the JDBC insert to complete for all " +
                    "the records sent.",
            defaultValue = "true",
            displayPriority = 2)
    public void setFlushOnSend(final boolean flushOnSend) {
        this.flushOnSend = flushOnSend;
    }

    private void log(final Severity severity, final String message, final Exception e) {
        errorReceiverProxy.log(severity, locationFactory.create(locator), getElementId(), message, e);
        switch (severity) {
            case FATAL_ERROR -> LOGGER.error(message, e);
            case ERROR -> LOGGER.error(message, e);
            case WARNING -> LOGGER.warn(message, e);
            case INFO -> LOGGER.info(message, e);
        }
    }
}
