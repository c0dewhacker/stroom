package stroom.index.lucene980;

import stroom.index.lucene980.SearchExpressionQueryBuilder.SearchExpressionQuery;
import stroom.index.lucene980.analyser.AnalyzerFactory;
import stroom.index.shared.IndexField;
import stroom.index.shared.IndexFieldsMap;
import stroom.query.api.v2.SearchRequest;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import jakarta.inject.Inject;
import org.apache.lucene980.analysis.Analyzer;

import java.util.HashMap;
import java.util.Map;

class SearchExpressionQueryCache {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SearchExpressionQueryCache.class);

    private final SearchExpressionQueryBuilderFactory searchExpressionQueryBuilderFactory;
    private final IndexFieldsMap indexFieldsMap = new IndexFieldsMap();
    private final Map<String, Analyzer> analyzerMap = new HashMap<>();
    private SearchExpressionQuery luceneQuery;

    @Inject
    SearchExpressionQueryCache(final SearchExpressionQueryBuilderFactory searchExpressionQueryBuilderFactory) {
        this.searchExpressionQueryBuilderFactory = searchExpressionQueryBuilderFactory;
    }

    SearchExpressionQuery getQuery(final SearchRequest searchRequest, final boolean ignoreMissingFields) {
        try {
            if (luceneQuery == null) {
                final SearchExpressionQueryBuilder searchExpressionQueryBuilder =
                        searchExpressionQueryBuilderFactory.create(
                                indexFieldsMap,
                                searchRequest.getDateTimeSettings());
                luceneQuery = searchExpressionQueryBuilder
                        .buildQuery(searchRequest.getQuery().getExpression());
            }
            return luceneQuery;
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
            throw e;
        }
    }

    Analyzer getAnalyser(final IndexField indexField) {
        try {
            Analyzer fieldAnalyzer = analyzerMap.get(indexField.getFieldName());
            if (fieldAnalyzer == null) {
                // Add the field analyser.
                fieldAnalyzer = AnalyzerFactory.create(indexField.getAnalyzerType(),
                        indexField.isCaseSensitive());
                analyzerMap.put(indexField.getFieldName(), fieldAnalyzer);
            }
            return fieldAnalyzer;
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
            throw e;
        }
    }

    void addIndexField(final IndexField indexField) {
        if (indexFieldsMap.putIfAbsent(indexField) == null) {
            // We didn't already have this field so make sure the query is rebuilt.
            luceneQuery = null;
        }
    }
}
