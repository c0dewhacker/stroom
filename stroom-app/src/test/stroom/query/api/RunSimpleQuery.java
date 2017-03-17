package stroom.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientResponse;
import org.hsqldb.ExpressionOp;
import org.junit.Test;
import stroom.dashboard.shared.Search;
import stroom.datasource.api.DataSource;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * This is not currently a test. It is a way of exercising the query api, i.e. it is support for manual testing.
 */
public class RunSimpleQuery {

    @Test
    public void test() throws JsonProcessingException {
        // Given
        SearchRequest searchRequest = getSearchRequest();
        String serialisedSearchRequest = serialiseSearchRequest(searchRequest);

        // When
        Client client = ClientBuilder.newClient(new ClientConfig().register(ClientResponse.class));
        Response response = client
                .target("http://localhost:8080/api/index/search")
                .request()
                .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
                .post(Entity.json(serialisedSearchRequest));
        System.out.println(response.toString());
    }

    private static SearchRequest getSearchRequest(){
        QueryKey queryKey = new QueryKey("Some UUID");
        Query query = new Query(
                new DocRef("docRefType", "docRefUuid", "docRefName"),
                new ExpressionBuilder(ExpressionOperator.Op.AND)
                        .addTerm("field1", ExpressionTerm.Condition.EQUALS, "value1")
                        .addOperator(ExpressionOperator.Op.AND)
                        .addTerm("field2", ExpressionTerm.Condition.BETWEEN, "value2")
                        .build()
        );

        List<ResultRequest> resultRequestList = new ArrayList<>();
        String datetimeLocale = "en-gb";
        boolean incremental = false;
        SearchRequest searchRequest = new SearchRequest(queryKey, query, resultRequestList, datetimeLocale, incremental);
        return searchRequest;
    }

    private static ObjectMapper getMapper(final boolean indent) {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, indent);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Enabling default typing adds type information where it would otherwise be ambiguous, i.e. for abstract classes
//        mapper.enableDefaultTyping();
        return mapper;
    }

    private static String serialiseSearchRequest(SearchRequest searchRequest) throws JsonProcessingException {
        ObjectMapper objectMapper = getMapper(true);
        return objectMapper.writeValueAsString(searchRequest);
    }
}
