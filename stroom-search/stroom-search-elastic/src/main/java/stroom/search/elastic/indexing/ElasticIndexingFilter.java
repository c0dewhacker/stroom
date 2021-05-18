/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.search.elastic.indexing;

import stroom.pipeline.server.LocationFactoryProxy;
import stroom.pipeline.server.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.server.errorhandler.ErrorStatistics;
import stroom.pipeline.server.errorhandler.LoggedException;
import stroom.pipeline.server.factory.ConfigurableElement;
import stroom.pipeline.server.factory.PipelineProperty;
import stroom.pipeline.server.factory.PipelinePropertyDocRef;
import stroom.pipeline.server.filter.AbstractXMLFilter;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.query.api.v2.DocRef;
import stroom.search.elastic.ElasticClientCache;
import stroom.search.elastic.ElasticClusterStore;
import stroom.search.elastic.ElasticIndexCache;
import stroom.search.elastic.ElasticIndexService;
import stroom.search.elastic.shared.ElasticCluster;
import stroom.search.elastic.shared.ElasticConnectionConfig;
import stroom.search.elastic.shared.ElasticIndex;
import stroom.search.elastic.shared.ElasticIndexField;
import stroom.search.elastic.shared.ElasticIndexFieldType;
import stroom.util.date.DateUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.Severity;
import stroom.util.spring.StroomScope;

import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Takes index XML and sends documents to Elasticsearch for indexing
 */
@Component
@Scope(StroomScope.PROTOTYPE)
@ConfigurableElement(type = "ElasticIndexingFilter", category = Category.FILTER, roles = {PipelineElementType.ROLE_TARGET,
        PipelineElementType.ROLE_HAS_TARGETS, PipelineElementType.VISABILITY_SIMPLE}, icon = ElementIcons.ELASTIC_INDEX)
class ElasticIndexingFilter extends AbstractXMLFilter {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ElasticIndexingFilter.class);

    private static final String RECORD_ELEMENT_NAME = "record";
    private static final String DATA_ELEMENT_NAME = "data";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VALUE_ATTRIBUTE = "value";

    private final LocationFactoryProxy locationFactory;
    private final ErrorReceiverProxy errorReceiverProxy;
    private final ElasticIndexCache elasticIndexCache;
    private final ElasticClientCache elasticClientCache;
    private final ElasticClusterStore elasticClusterStore;
    private final ElasticIndexService elasticIndexService;

    private Map<String, ElasticIndexField> fieldsMap;
    private ElasticIndex elasticIndex;
    private DocRef indexRef;
    private Collection<Map<String, Object>> currentDocuments = new ArrayList<>();
    private Map<String, Object> document = new HashMap<>();
    private List<String> currentStringArray;
    private String currentArrayString;
    private List<Map<String, Object>> currentObjectArray;
    private Map<String, Object> currentArrayObject;
    private boolean isBuildingArrayObject = false;
    private Map<String, Object> currentObject;
    private boolean isBuildingObject = false;
    private String currentPropertyName;

    private int batchSize = 10000;
    private boolean refreshAfterEachBatch = false;
    private int fieldsIndexed;
    private long docsIndexed;

    private Locator locator;

    @Inject
    ElasticIndexingFilter(final LocationFactoryProxy locationFactory,
                          final ErrorReceiverProxy errorReceiverProxy,
                          final ElasticIndexCache elasticIndexCache,
                          final ElasticClientCache elasticClientCache,
                          final ElasticClusterStore elasticClusterStore,
                          final ElasticIndexService elasticIndexService
    ) {
        this.locationFactory = locationFactory;
        this.errorReceiverProxy = errorReceiverProxy;
        this.elasticIndexCache = elasticIndexCache;
        this.elasticClientCache = elasticClientCache;
        this.elasticClusterStore = elasticClusterStore;
        this.elasticIndexService = elasticIndexService;
    }

    /**
     * Initialise
     */
    @Override
    public void startProcessing() {
        try {
            if (indexRef == null) {
                log(Severity.FATAL_ERROR, "Index has not been set", null);
                throw new LoggedException("Index has not been set");
            }

            // Get the index and index fields from the cache.
            elasticIndex = elasticIndexCache.get(indexRef);
            if (elasticIndex == null) {
                log(Severity.FATAL_ERROR, "Unable to load index", null);
                throw new LoggedException("Unable to load index");
            }

            fieldsMap = elasticIndexService.getFieldsMap(elasticIndex);

            final ElasticCluster elasticCluster = elasticClusterStore.readDocument(elasticIndex.getClusterRef());
            final ElasticConnectionConfig connectionConfig = elasticCluster.getConnectionConfig();

            elasticClientCache.context(connectionConfig, elasticClient -> {
                try {
                    final boolean pingSucceeded = elasticClient.ping(RequestOptions.DEFAULT);
                    if (pingSucceeded) {
                        LOGGER.debug(() -> "Ping to Elasticsearch cluster: '" + connectionConfig.getConnectionUrls() + "' succeeded");
                    }
                    else {
                        throw new IOException("Failed to ping Elasticsearch cluster: '" + connectionConfig.getConnectionUrls() + "'");
                    }
                } catch (final IOException | RuntimeException e) {
                    log(Severity.FATAL_ERROR, e.getMessage(), e);
                    // Terminate processing as this is a fatal error.
                    throw new LoggedException(e.getMessage(), e);
                }
            });
        } finally {
            super.startProcessing();
        }
    }

    @Override
    public void endProcessing() {
        try {
            // Send last docs.
            addDocuments(currentDocuments);
            currentDocuments = null;
        } finally {
            super.endProcessing();
        }
    }

    /**
     * Sets the locator to use when reporting errors.
     *
     * @param locator The locator to use.
     */
    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator;
        super.setDocumentLocator(locator);
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        if (DATA_ELEMENT_NAME.equals(localName) && document != null) {
            String name = attributes.getValue(NAME_ATTRIBUTE);
            String value = attributes.getValue(VALUE_ATTRIBUTE);

            if (name == null) {
                if (value == null) {
                    // Start of an array object
                    currentArrayObject = new HashMap<>();
                    currentObjectArray.add(currentArrayObject);
                } else {
                    // Node with no name, but a value. Treat this as a string array item.
                    if (currentStringArray != null) {
                        if (currentArrayObject != null) {
                            throw new SAXException("Cannot mix strings and objects in arrays");
                        }

                        currentArrayString = value;
                        currentStringArray.add(value);
                    }
                }
            } else {
                if (value == null) {
                    // Node with a name and no value. Treat this as the start of an array.
                    // Could be either a string or object array.
                    if (currentStringArray != null && currentStringArray.size() > 0 ||
                        currentObjectArray != null && currentObjectArray.size() > 0
                    ) {
                        throw new SAXException("Cannot nest an array within another array");
                    }

                    currentStringArray = new ArrayList<>();
                    currentObjectArray = new ArrayList<>();
                    currentObject = new HashMap<>();
                    currentPropertyName = name;
                } else {
                    if (currentArrayObject != null) {
                        // An array object has been started, so treat this key/value pair as a property of that object
                        isBuildingArrayObject = true;
                        currentArrayObject.put(name, value);
                    } else if (currentObject != null) {
                        // An object has been started, so apply this key/value pair to the object
                        isBuildingObject = true;
                        currentObject.put(name, value);
                    } else {
                        // This is a simple property, not part of an array
                        if (name.length() > 0 && value.length() > 0) {
                            // See if we can get this field.
                            final ElasticIndexField indexField = fieldsMap.get(name);
                            if (indexField != null) {
                                // Index the current content if we are to store or index
                                // this field.
                                if (indexField.isIndexed() || indexField.isStored()) {
                                    addFieldToDocument(indexField, value);
                                }
                            } else {
                                final String msg = "No explicit field mapping exists for field: '" + name + "'";
                                LOGGER.debug(() -> msg);

                                // Add the field to the document by name, so it's available for dynamic property mappings
                                // and included in the document `_source` field
                                addFieldToDocument(name, value);
                            }
                        }
                    }
                }
            }
        } else if (RECORD_ELEMENT_NAME.equals(localName)) {
            // Create a document to store fields in.
            document = new HashMap<>();
        }

        super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (DATA_ELEMENT_NAME.equals(localName)) {
            if (isBuildingArrayObject) {
                // An array object is currently being built and is not yet completed
                isBuildingArrayObject = false;
            } else if (isBuildingObject) {
                isBuildingObject = false;
            } else if (currentArrayObject != null) {
                // An array object has ended
                currentArrayObject = null;
            } else if (currentArrayString != null) {
                // An array string has ended
                currentArrayString = null;
            } else {
                if (currentObjectArray != null) {
                    // We're at the end of an object array, so commit it to the document
                    if (currentObjectArray.size() > 0) {
                        addFieldToDocument(currentPropertyName, currentObjectArray);
                    }
                    currentObjectArray = null;
                    currentArrayObject = null;
                }
                if (currentStringArray != null) {
                    // End of a string array
                    if (currentStringArray.size() > 0) {
                        addFieldToDocument(currentPropertyName, currentStringArray);
                    }
                    currentStringArray = null;
                }
                if (currentObject != null) {
                    // End of plain object
                    if (currentObject.size() > 0) {
                        addFieldToDocument(currentPropertyName, currentObject);
                    }
                    currentObject = null;
                }
            }
        } else if (RECORD_ELEMENT_NAME.equals(localName)) {
            processDocument();
            document = null;
            currentStringArray = null;
            currentArrayString = null;
            currentObjectArray = null;
            currentArrayObject = null;
            currentObject = null;
            isBuildingArrayObject = false;
            isBuildingObject = false;

            // Reset the count of how many fields we have indexed for the
            // current event.
            fieldsIndexed = 0;

            if (errorReceiverProxy.getErrorReceiver() != null &&
                errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics
            ) {
                ((ErrorStatistics) errorReceiverProxy.getErrorReceiver()).checkRecord(-1);
            }
        }

        super.endElement(uri, localName, qName);
    }

    private void processDocument() {
        // Write the document if we have dropped out of the record element and
        // have indexed some fields.
        if (fieldsIndexed > 0) {
            docsIndexed++;
            currentDocuments.add(document);
            document = new HashMap<>();

            if (currentDocuments.size() > batchSize) {
                addDocuments(currentDocuments);
                currentDocuments = new ArrayList<>();
            }
        }
    }

    /**
     * Index the current batch of documents
     */
    private void addDocuments(final Collection<Map<String, Object>> documents) {
        if (docsIndexed > 0) {
            final String indexName = elasticIndex.getIndexName();
            final ElasticCluster elasticCluster = elasticClusterStore.readDocument(elasticIndex.getClusterRef());

            elasticClientCache.context(elasticCluster.getConnectionConfig(), elasticClient -> {
                try {
                    if (documents.size() > 0) {
                        // Create a new bulk indexing request, containing the current batch of documents
                        BulkRequest bulkRequest = new BulkRequest();

                        // For each document, create an indexing request and append to the bulk request
                        bulkRequest.add(
                                documents.stream().map(document -> new IndexRequest(indexName)
                                        .opType(OpType.CREATE)
                                        .source(document)).collect(Collectors.toList())
                        );

                        if (refreshAfterEachBatch) {
                            // Refresh upon completion of the batch index request
                            bulkRequest.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
                        }
                        else {
                            // Only refresh after all batches have been indexed
                            bulkRequest.setRefreshPolicy(RefreshPolicy.NONE);
                        }

                        BulkResponse response = elasticClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                        if (response.hasFailures()) {
                            throw new IOException("Bulk index request failed: " + response.buildFailureMessage());
                        }
                        else {
                            LOGGER.info(() -> "Indexed " + documents.size() + " items to '" + indexName + "' in " +
                                response.getTook().getSecondsFrac() + " seconds");
                        }
                    }
                } catch (final RuntimeException | IOException e) {
                    log(Severity.FATAL_ERROR, e.getMessage(), e);

                    // Terminate processing as this is a fatal error.
                    throw new LoggedException(e.getMessage(), e);
                }
            });
        }
    }

    /**
     * Adds a field and value to the current document
     * @param indexField Represents the field mapping
     * @param value String-based value
     */
    private void addFieldToDocument(final ElasticIndexField indexField, final String value) {
        try {
            Object val = null;
            final ElasticIndexFieldType elasticFieldType = indexField.getFieldUse();

            if (elasticFieldType.isNumeric()) {
                val = Long.parseLong(value);
            } else if (elasticFieldType.isDecimal()) {
                val = Float.parseFloat(value);
            } else if (ElasticIndexFieldType.DATE.equals(elasticFieldType)) {
                try {
                    val = DateUtil.parseUnknownString(value);
                } catch (final Exception e) {
                    LOGGER.trace(e::getMessage, e);
                }
            } else {
                val = value;
            }

            // Add the current field to the document if it is not null.
            if (val != null) {
                LOGGER.debug(() -> "processIndexContent() - Adding to index indexName=" +
                    indexRef.getName() +
                    " name=" +
                    indexField.getFieldName() +
                    " value=" +
                    value);

                fieldsIndexed++;
                document.put(indexField.getFieldName(), val);
            }
        } catch (final RuntimeException e) {
            log(Severity.ERROR, e.getMessage(), e);
        }
    }

    /**
     * Adds a field and value to the document, where an existing mapping is not defined.
     * If Elasticsearch index dynamic properties are defined, a field mapping will be created if the field name matches
     * the defined pattern. Regardless, the field will be added to the document's `_source`.
     */
    private void addFieldToDocument(final String fieldName, final Object value) {
        if (!document.containsKey(fieldName)) {
            LOGGER.debug(() -> "processIndexContent() - Adding to index indexName=" +
                indexRef.getName() +
                " name=" +
                fieldName +
                " value=" +
                value
            );

            document.put(fieldName, value);
            fieldsIndexed++;
        }
        else {
            LOGGER.warn(() -> "Field '" + fieldName + "' already exists in document");
        }
    }

    @PipelineProperty(description = "The index to send records to")
    @PipelinePropertyDocRef(types = ElasticIndex.ENTITY_TYPE)
    public void setIndex(final DocRef indexRef) {
        this.indexRef = indexRef;
    }

    @PipelineProperty(description = "How many documents to send to the index in a single post", defaultValue = "10000")
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    @PipelineProperty(description = "Refresh the index after each batch is processed, making the indexed documents visible to searches", defaultValue = "false")
    public void setRefreshAfterEachBatch(final boolean refreshAfterEachBatch) {
        this.refreshAfterEachBatch = refreshAfterEachBatch;
    }

    private void log(final Severity severity, final String message, final Exception e) {
        errorReceiverProxy.log(severity, locationFactory.create(locator), getElementId(), message, e);
    }
}
