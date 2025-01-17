package stroom.query.language;

import stroom.expression.api.DateTimeSettings;
import stroom.expression.api.ExpressionContext;
import stroom.query.api.v2.Query;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.SearchRequest;
import stroom.query.language.token.AbstractQueryTest;
import stroom.util.json.JsonUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestSearchRequestBuilder extends AbstractQueryTest {

    @Override
    protected Path getTestDir() {
        final Path dir = Paths.get("../stroom-query-common/src/test/resources/TestSearchRequestBuilder");
        if (!Files.isDirectory(dir)) {
            throw new RuntimeException("Test data directory not found: " + dir.toAbsolutePath());
        }
        return dir;
    }

    @Override
    protected String convert(final String input) {
        try {
            final List<ResultRequest> resultRequests = new ArrayList<>(0);
            final QueryKey queryKey = new QueryKey("test");
            final Query query = Query.builder().build();
            final DateTimeSettings dateTimeSettings = DateTimeSettings.builder().referenceTime(0L).build();
            SearchRequest searchRequest = new SearchRequest(
                    null,
                    queryKey,
                    query,
                    resultRequests,
                    dateTimeSettings,
                    false);
            final ExpressionContext expressionContext = ExpressionContext
                    .builder()
                    .dateTimeSettings(dateTimeSettings)
                    .maxStringLength(100)
                    .build();
            searchRequest = new SearchRequestBuilder(
                    (keywordGroup, parentTableSettings) -> null,
                    new MockDocResolver())
                    .create(input, searchRequest, expressionContext);
            return JsonUtil.writeValueAsString(searchRequest);

        } catch (final RuntimeException e) {
            return e.toString();
        }
    }
}
