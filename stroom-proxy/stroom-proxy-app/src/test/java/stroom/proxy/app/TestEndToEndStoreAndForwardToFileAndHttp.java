package stroom.proxy.app;

import stroom.proxy.app.DbRecordCountAssertion.DbRecordCounts;
import stroom.proxy.repo.AggregatorConfig;
import stroom.proxy.repo.ProxyRepoConfig;
import stroom.receive.common.ReceiveDataConfig;
import stroom.security.openid.api.IdpType;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.time.StroomDuration;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class TestEndToEndStoreAndForwardToFileAndHttp extends AbstractEndToEndTest {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(
            TestEndToEndStoreAndForwardToFileAndHttp.class);

    @Inject
    private DbRecordCountAssertion dbRecordCountAssertion;
    @Inject
    private MockFileDestination mockFileDestination;

    @Override
    protected ProxyConfig getProxyConfigOverride() {
        return ProxyConfig.builder()
                .proxyId("TestProxy")
                .pathConfig(createProxyPathConfig())
                .proxyRepoConfig(ProxyRepoConfig.builder()
                        .storingEnabled(true)
                        .build())
                .securityConfig(new ProxySecurityConfig(ProxyAuthenticationConfig.builder()
                        .openIdConfig(new ProxyOpenIdConfig()
                                .withIdentityProviderType(IdpType.TEST_CREDENTIALS))
                        .build()))
                .aggregatorConfig(AggregatorConfig.builder()
                        .maxUncompressedByteSizeString("1G")
                        .maxAggregateAge(StroomDuration.ofSeconds(5))
                        .aggregationFrequency(StroomDuration.ofSeconds(1))
                        .maxItemsPerAggregate(3)
                        .build())
                .addForwardDestination(MockFileDestination.createForwardFileConfig()) // forward to file and http
                .addForwardDestination(MockHttpDestination.createForwardHttpPostConfig())
                .feedStatusConfig(MockHttpDestination.createFeedStatusConfig())
                .receiveDataConfig(ReceiveDataConfig.builder()
                        .withAuthenticationRequired(false)
                        .build())
                .build();
    }

    @Test
    void test() {
        LOGGER.info("Starting basic end-end test");
        dbRecordCountAssertion.assertRecordCounts(new DbRecordCounts(0, 0, 0, 2, 0, 0, 0, 0));

        mockHttpDestination.setupStroomStubs(mappingBuilder ->
                mappingBuilder.willReturn(WireMock.ok()));
        // now the stubs are set up wait for proxy to be ready as proxy needs the
        // stubs to be available to be healthy
        waitForHealthyProxyApp(Duration.ofSeconds(30));

        // Two feeds each send 4, agg max items of 3 so two batches each
        final PostDataHelper postDataHelper = createPostDataHelper();
        for (int i = 0; i < 4; i++) {
            postDataHelper.sendTestData1();
            postDataHelper.sendTestData2();
        }

        Assertions.assertThat(postDataHelper.getPostCount())
                .isEqualTo(8);

        // Assert file contents.
        mockFileDestination.assertFileContents(getConfig());

        dbRecordCountAssertion.assertRecordCounts(new DbRecordCounts(0, 2, 0, 2, 0, 0, 0, 0));

        // Health check sends in a feed status check with DUMMY_FEED to see if stroom is available
        mockHttpDestination.assertFeedStatusCheck();

        // Check the HTTP forwarding
        mockHttpDestination.assertRequestCount(4);

        // Assert the posts.
        mockHttpDestination.assertPosts();

        dbRecordCountAssertion.assertRecordCounts(new DbRecordCounts(0, 2, 0, 2, 0, 0, 0, 0));

        // Health check sends in a feed status check with DUMMY_FEED to see if stroom is available
        mockHttpDestination.assertFeedStatusCheck();
    }
}
