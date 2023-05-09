package stroom.app.guice;

import stroom.app.errors.NodeCallExceptionMapper;
import stroom.dropwizard.common.PermissionExceptionMapper;
import stroom.dropwizard.common.TokenExceptionMapper;
import stroom.security.api.SecurityContext;
import stroom.security.api.UserIdentity;
import stroom.security.api.UserIdentityFactory;
import stroom.util.NullSafe;
import stroom.util.guice.GuiceUtil;
import stroom.util.jersey.JerseyClientFactory;
import stroom.util.jersey.JerseyClientName;
import stroom.util.jersey.WebTargetFactory;
import stroom.util.jersey.WebTargetProxy;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.util.Map;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ExceptionMapper;

public class JerseyModule extends AbstractModule {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(JerseyModule.class);

    @Override
    protected void configure() {
        bind(JerseyClientFactory.class).to(JerseyClientFactoryImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ExceptionMapper.class)
                .addBinding(NodeCallExceptionMapper.class)
                .addBinding(PermissionExceptionMapper.class)
                .addBinding(TokenExceptionMapper.class);
    }

    /**
     * Provides a Jersey {@link WebTargetFactory} with added Authorization header containing the
     * user's access token.
     * Only use this when you want {@link WebTarget} with the Authorization header already added to it,
     * else just get a Client and create a {@link WebTarget} from that.
     */
    @SuppressWarnings("unused") // Guice injected
    @Provides
    @Singleton
    WebTargetFactory provideJerseyRequestBuilder(final JerseyClientFactoryImpl jerseyClientFactory,
                                                 final SecurityContext securityContext,
                                                 final Provider<UserIdentityFactory> userIdentityFactoryProvider) {
        return url -> {
            final JerseyClientName clientName = JerseyClientName.STROOM;
            final Client client = jerseyClientFactory.getNamedClient(clientName);
            final WebTarget delegateWebTarget = client.target(url);
            LOGGER.debug("Building WebTarget for client: '{}', url: '{}'", clientName, url);
            return (WebTarget) new WebTargetProxy(delegateWebTarget) {
                @Override
                public Builder request() {
                    final Builder builder = super.request();
                    addAuthHeader(builder);
                    return builder;
                }

                @Override
                public Builder request(final String... acceptedResponseTypes) {
                    final Builder builder = super.request(acceptedResponseTypes);
                    addAuthHeader(builder);
                    return builder;
                }

                @Override
                public Builder request(final MediaType... acceptedResponseTypes) {
                    final Builder builder = super.request(acceptedResponseTypes);
                    addAuthHeader(builder);
                    return builder;
                }

                private void addAuthHeader(final Builder builder) {
                    final UserIdentity userIdentity = securityContext.getUserIdentity();
                    final Map<String, String> authHeaders = userIdentityFactoryProvider.get()
                            .getAuthHeaders(userIdentity);
                    LOGGER.debug(() -> LogUtil.message("Adding auth headers to request, keys: '{}', userType: {}",
                            String.join(", ", NullSafe.map(authHeaders).keySet()),
                            NullSafe.get(userIdentity, Object::getClass, Class::getSimpleName)));
                    authHeaders.forEach(builder::header);
                }
            };
        };
    }
}
