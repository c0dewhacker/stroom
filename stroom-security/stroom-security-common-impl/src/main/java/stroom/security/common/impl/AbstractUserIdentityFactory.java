package stroom.security.common.impl;

import stroom.security.api.HasJwt;
import stroom.security.api.HasSession;
import stroom.security.api.ServiceUserFactory;
import stroom.security.api.UserIdentity;
import stroom.security.api.UserIdentityFactory;
import stroom.security.api.exception.AuthenticationException;
import stroom.security.openid.api.IdpType;
import stroom.security.openid.api.OpenId;
import stroom.security.openid.api.OpenIdConfiguration;
import stroom.security.openid.api.TokenResponse;
import stroom.util.NullSafe;
import stroom.util.authentication.DefaultOpenIdCredentials;
import stroom.util.authentication.HasRefreshable;
import stroom.util.authentication.Refreshable;
import stroom.util.cert.CertificateExtractor;
import stroom.util.exception.ThrowingFunction;
import stroom.util.jersey.JerseyClientFactory;
import stroom.util.jersey.JerseyClientName;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.lifecycle.Managed;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public abstract class AbstractUserIdentityFactory implements UserIdentityFactory, Managed {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(AbstractUserIdentityFactory.class);

    private final JwtContextFactory jwtContextFactory;
    private final Provider<OpenIdConfiguration> openIdConfigProvider;
    private final DefaultOpenIdCredentials defaultOpenIdCredentials;
    private final CertificateExtractor certificateExtractor;
    //    private final ProcessingUserIdentityProvider processingUserIdentityProvider;
    private final ServiceUserFactory serviceUserFactory;
    private final JerseyClientFactory jerseyClientFactory;

    // A service account/user for communicating with other apps in the same OIDC realm,
    // e.g. proxy => stroom. Created lazily.
    // This is tied to stroom/proxy's clientId, and we have only one of them
    private volatile UserIdentity serviceUserIdentity;

    //    private final BlockingQueue<AbstractTokenUserIdentity> refreshTokensDelayQueue = new DelayQueue<>();
    private final BlockingQueue<Refreshable> updatableTokensDelayQueue = new DelayQueue<>();
    private ExecutorService refreshExecutorService = null;
    private final AtomicBoolean isShutdownInProgress = new AtomicBoolean(false);
    // Don't change the configuration of this mapper after it is created, else not thread safe
    private final ObjectMapper objectMapper;
    private final IdpType idpType;

    public AbstractUserIdentityFactory(final JwtContextFactory jwtContextFactory,
                                       final Provider<OpenIdConfiguration> openIdConfigProvider,
                                       final DefaultOpenIdCredentials defaultOpenIdCredentials,
                                       final CertificateExtractor certificateExtractor,
//                                       final ProcessingUserIdentityProvider processingUserIdentityProvider,
                                       final ServiceUserFactory serviceUserFactory,
                                       final JerseyClientFactory jerseyClientFactory) {
        this.jwtContextFactory = jwtContextFactory;
        this.openIdConfigProvider = openIdConfigProvider;
        this.defaultOpenIdCredentials = defaultOpenIdCredentials;
        this.certificateExtractor = certificateExtractor;
//        this.processingUserIdentityProvider = processingUserIdentityProvider;
        this.serviceUserFactory = serviceUserFactory;
        this.jerseyClientFactory = jerseyClientFactory;
        this.objectMapper = createObjectMapper();
        // Bake this in as a restart is required for this prop
        this.idpType = openIdConfigProvider.get().getIdentityProviderType();
    }

    /**
     * Map the IDP identity provided by the {@link JwtContext} to a local user.
     *
     * @param jwtContext The identity on the IDP to map to a local user.
     * @param request    The HTTP request
     * @return A local {@link UserIdentity} if the identity can be mapped.
     */
    protected abstract Optional<UserIdentity> mapApiIdentity(final JwtContext jwtContext,
                                                             final HttpServletRequest request);

    /**
     * Map the IDP identity provided by the {@link JwtContext} and the
     * {@link TokenResponse}to a local user. This is for use in a UI based
     * authentication flow.
     *
     * @param jwtContext    The identity on the IDP to map to a local user.
     * @param request       The HTTP request
     * @param tokenResponse The token received from the IDP.
     * @return A local {@link UserIdentity} if the identity can be mapped.
     */
    protected abstract Optional<UserIdentity> mapAuthFlowIdentity(final JwtContext jwtContext,
                                                                  final HttpServletRequest request,
                                                                  final TokenResponse tokenResponse);

    @Override
    public Optional<UserIdentity> getApiUserIdentity(final HttpServletRequest request) {
        Optional<UserIdentity> optUserIdentity = Optional.empty();

        if (IdpType.NO_IDP.equals(idpType)) {
            throw new IllegalStateException(
                    "Attempting to get user identity from tokens in request when " +
                            "identityProviderType set to NONE.");
        } else {
            // See if we can log in with a token if one is supplied. It is valid for it to not be present.
            // e.g. the front end calling API methods, as the user is held in session.
            try {
                final Optional<JwtContext> optJwtContext = jwtContextFactory.getJwtContext(request);

                optUserIdentity = optJwtContext.flatMap(jwtContext ->
                                mapApiIdentity(jwtContext, request))
                        .or(() -> {
                            LOGGER.trace(() ->
                                    "No JWS found in headers in request to " + request.getRequestURI());
                            return Optional.empty();
                        });
            } catch (final RuntimeException e) {
                throw new AuthenticationException("Error authenticating request to "
                        + request.getRequestURI() + " - " + e.getMessage(), e);
            }

            if (optUserIdentity.isEmpty()) {
                LOGGER.trace(() -> "Cannot get a valid JWS for API request to " + request.getRequestURI());
            } else {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Got API user identity "
                            + optUserIdentity.map(Objects::toString).orElse("EMPTY"));
                }
            }

        }
        return optUserIdentity;
    }

    @Override
    public boolean hasAuthenticationToken(final HttpServletRequest request) {
        return jwtContextFactory.hasToken(request);
    }

    @Override
    public boolean hasAuthenticationCertificate(final HttpServletRequest request) {
        return certificateExtractor.extractCertificate(request).isPresent();
    }

    @Override
    public void removeAuthEntries(final Map<String, String> headers) {
        jwtContextFactory.removeAuthorisationEntries(headers);
    }

    @Override
    public Map<String, String> getServiceUserAuthHeaders() {
        if (IdpType.NO_IDP.equals(idpType)) {
            LOGGER.debug("IdpType is {}", idpType);
            return Collections.emptyMap();
        } else {
            final UserIdentity serviceUserIdentity = getServiceUserIdentity();
            return getAuthHeaders(serviceUserIdentity);
        }
    }

    @Override
    public Map<String, String> getAuthHeaders(final UserIdentity userIdentity) {

        if (userIdentity == null) {
            LOGGER.debug("Null user supplied");
            return Collections.emptyMap();

        } else {
            LOGGER.debug(() -> LogUtil.message("IdpType: {}, userIdentity type: {}",
                    idpType, userIdentity.getClass().getSimpleName()));

            if (IdpType.NO_IDP.equals(idpType)) {
                return Collections.emptyMap();

            } else if (IdpType.TEST_CREDENTIALS.equals(idpType)
                    && !serviceUserFactory.isServiceUser(userIdentity, getServiceUserIdentity())) {
                // The processing user is a bit special so even when using hard-coded default open id
                // creds the proc user uses tokens created by the internal IDP.
                LOGGER.debug("Using default token");
                return jwtContextFactory.createAuthorisationEntries(defaultOpenIdCredentials.getApiKey());

            } else if (userIdentity instanceof final HasUpdatableToken hasUpdatableToken) {
                LOGGER.debug(() -> LogUtil.message("Getting auth headers as {}, {}",
                        HasUpdatableToken.class.getSimpleName(),
                        userIdentity.getClass().getSimpleName()));
                // Ensure the token hasn't gone off, just in case the refresh queue (which refreshes ahead of the
                // expiry time) is busy, so the call to refresh is unlikely.
                final UpdatableToken updatableToken = hasUpdatableToken.getUpdatableToken();

                updatableToken.refreshIfRequired();
                final String accessToken = Objects.requireNonNull(updatableToken.getAccessToken(),
                        () -> "Null access token for userIdentity " + userIdentity);
                return jwtContextFactory.createAuthorisationEntries(accessToken);

            } else if (userIdentity instanceof final HasJwt hasJwt) {
                LOGGER.debug(() -> LogUtil.message("Getting auth headers as {}, {}",
                        HasJwt.class.getSimpleName(),
                        userIdentity.getClass().getSimpleName()));
                // This is for stroom's internal IDP processing user identity (which we don't need to refresh as
                // ProcessingUserIdentityProviderImpl handles that) or for users that have come from
                // an AWS ALB with an access token that we don't update.
                final String accessToken = Objects.requireNonNull(hasJwt.getJwt());
                return jwtContextFactory.createAuthorisationEntries(accessToken);

            } else {
                LOGGER.debug(() -> "Wrong type of userIdentity " + userIdentity.getClass());
                return Collections.emptyMap();
            }
        }
    }

    @Override
    public Map<String, String> getAuthHeaders(final String jwt) {
        return jwtContextFactory.createAuthorisationEntries(jwt);
    }

    /**
     * Extracts the authenticated user's identity from http request when that
     * request is part of a UI based authentication flow with the IDP
     */
    public Optional<UserIdentity> getAuthFlowUserIdentity(final HttpServletRequest request,
                                                          final String code,
                                                          final AuthenticationState state) {
        final OpenIdConfiguration openIdConfiguration = openIdConfigProvider.get();

        if (IdpType.NO_IDP.equals(idpType)) {
            throw new IllegalStateException(
                    "Attempting to do OIDC auth flow with identityProviderType set to NONE.");
        }

        final String tokenEndpoint = openIdConfiguration.getTokenEndpoint();

        final TokenResponse tokenResponse = new OpenIdTokenRequestHelper(
                tokenEndpoint, openIdConfiguration, objectMapper, jerseyClientFactory)
                .withCode(code)
                .withGrantType(OpenId.GRANT_TYPE__AUTHORIZATION_CODE)
                .withRedirectUri(state.getRedirectUri())
                .sendRequest(true);

        final Optional<UserIdentity> optUserIdentity = jwtContextFactory.getJwtContext(tokenResponse.getIdToken())
                .flatMap(jwtContext ->
                        createUserIdentity(request, state, tokenResponse, jwtContext))
                .or(() -> {
                    throw new RuntimeException("Unable to extract JWT claims");
                });

        LOGGER.debug(() -> "Got auth flow user identity "
                + optUserIdentity.map(Objects::toString).orElse("EMPTY"));

        return optUserIdentity;
    }

    @Override
    public UserIdentity getServiceUserIdentity() {

        // Ideally the token will get recreated by the refresh queue just before
        // it expires so callers to this will find a token that is good to use and
        // thus won't be contended.
        if (serviceUserIdentity == null) {
            synchronized (this) {
                if (serviceUserIdentity == null) {
                    serviceUserIdentity = createServiceUserIdentity();
                }
            }
        }

        // Make sure it is up-to-date before giving it out
        if (serviceUserIdentity instanceof final HasUpdatableToken hasUpdatableToken) {
            final UpdatableToken updatableToken = hasUpdatableToken.getUpdatableToken();
            updatableToken.refreshIfRequired();
        }

        return serviceUserIdentity;
    }

    @Override
    public boolean isServiceUser(final UserIdentity userIdentity) {
        return serviceUserFactory.isServiceUser(userIdentity, getServiceUserIdentity());
//        final UserIdentity serviceUserIdentity = getServiceUserIdentity();
//        // Use instance equality check as there should only ever be one ServiceUserIdentity
//        // in this JVM
//        final boolean isServiceUserIdentity = userIdentity instanceof ServiceUserIdentity
//                && userIdentity == serviceUserIdentity;
//        LOGGER.debug("isServiceUserIdentity: {}, userIdentity: {}, serviceUserIdentity: {}",
//                isServiceUserIdentity, userIdentity, serviceUserIdentity);
//        return isServiceUserIdentity;
    }

    @Override
    public boolean isServiceUser(final String subject, final String issuer) {
        final UserIdentity processingUserIdentity = getServiceUserIdentity();
        if (processingUserIdentity instanceof final HasJwtClaims hasJwtClaims) {
            return Optional.ofNullable(hasJwtClaims.getJwtClaims())
                    .map(ThrowingFunction.unchecked(jwtClaims -> {
                        final boolean isProcessingUser = Objects.equals(subject, jwtClaims.getSubject())
                                && Objects.equals(issuer, jwtClaims.getIssuer());

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Comparing subject: [{}|{}], issuer[{}|{}], result: {}",
                                    subject,
                                    jwtClaims.getSubject(),
                                    issuer,
                                    jwtClaims.getIssuer(),
                                    isProcessingUser);
                        }
                        return isProcessingUser;
                    }))
                    .orElse(false);
        } else {
            final String requiredIssuer = openIdConfigProvider.get().getIssuer();
            final boolean isProcessingUser = Objects.equals(subject, processingUserIdentity.getSubjectId())
                    && Objects.equals(issuer, requiredIssuer);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Comparing subject: [{}|{}], issuer[{}|{}], result: {}",
                        subject,
                        processingUserIdentity.getSubjectId(),
                        issuer,
                        requiredIssuer,
                        isProcessingUser);
            }
            return isProcessingUser;
        }
    }

    /**
     * Refresh the user identity including any tokens associated with that user.
     *
     * @param userIdentity
     */
    public void refresh(final UserIdentity userIdentity) {
        Objects.requireNonNull(userIdentity, "Null userIdentity");
        if (userIdentity instanceof final HasUpdatableToken hasUpdatableToken) {

            // This will try and refresh/recreate the token just before it expires
            // so that there is no delay for users of the token. They can explicitly call
            // refresh after checking AbstractTokenUserIdentity.hasTokenExpired() if they don't trust
            // the refresh queue, but it should always return false.
            final UpdatableToken updatableToken = hasUpdatableToken.getUpdatableToken();
            final boolean didRefresh = updatableToken.refreshIfRequired();

            if (LOGGER.isTraceEnabled()) {
                if (!didRefresh) {
                    LOGGER.trace("Refresh not done for userIdentity: {}, updatableToken: {}",
                            userIdentity, updatableToken);
                }
            }
//
//            if (userIdentity instanceof ServiceUserIdentity) {
//                // service users do not have refresh tokens so just create new ones
//                didRefresh = tokenUserIdentity.refresh(
////                        AbstractTokenUserIdentity::isTokenRefreshRequired,
//                        userIdentity2 -> createOrUpdateServiceUserIdentity());
//            } else {
//                // This takes care of calling isRefreshRequired before and after getting a lock
//                didRefresh = tokenUserIdentity.refresh(
////                        AbstractTokenUserIdentity::isTokenRefreshRequired,
//                        this::refreshUsingRefreshToken);
//            }
        }
    }

    // Maybe ought to bake the refresh token claims into the UpdatableToken
    private boolean hasRefreshTokenExpired(final TokenResponse tokenResponse) {
        // At some point the refresh token itself will expire, so then we need to remove
        // the identity from the session if there is one to force the user to re-authenticate
        if (NullSafe.isBlankString(tokenResponse, TokenResponse::getRefreshToken)) {
            return false;
        } else {
            return jwtContextFactory.getJwtContext(tokenResponse.getRefreshToken(), false)
                    .map(JwtContext::getJwtClaims)
                    .map(ThrowingFunction.unchecked(JwtClaims::getExpirationTime))
                    .map(expireTime -> NumericDate.now().isAfter(expireTime))
                    .orElse(false);
        }
    }

    protected FetchTokenResult refreshUsingRefreshToken(final UpdatableToken updatableToken) {
        Objects.requireNonNull(updatableToken);
        final UserIdentity identity = Objects.requireNonNull(updatableToken.getUserIdentity());
        final TokenResponse currentTokenResponse = updatableToken.getTokenResponse();

        FetchTokenResult fetchTokenResult;

        if (hasRefreshTokenExpired(currentTokenResponse)) {
            if (identity instanceof final HasSession userWithSession) {
                LOGGER.info("Refresh token has expired, removing user identity from " +
                        "session to force re-authentication. userIdentity: {}", userWithSession);
                userWithSession.removeUserFromSession();
            } else {
                LOGGER.warn("Refresh token has expired, can't refresh token or create new.");
            }
            fetchTokenResult = null;
        } else {
            TokenResponse newTokenResponse = null;
            JwtClaims jwtClaims = null;
            try {
                LOGGER.debug("Refreshing token " + identity);

                LOGGER.debug(LogUtil.message(
                        "Current token expiry max age: {}, refresh token expiry max age: {}",
                        NullSafe.toString(updatableToken.getTokenResponse(),
                                TokenResponse::getExpiresIn,
                                Duration::ofSeconds),
                        NullSafe.toString(updatableToken.getTokenResponse(),
                                TokenResponse::getEffectiveRefreshExpiresIn,
                                Duration::ofSeconds)));

                fetchTokenResult = refreshTokens(currentTokenResponse);
                newTokenResponse = fetchTokenResult.tokenResponse();
                jwtClaims = fetchTokenResult.jwtClaims();
            } catch (final RuntimeException e) {
                LOGGER.error("Error refreshing token for {} - {}", identity, e.getMessage(), e);
                if (identity instanceof final HasSession userWithSession) {
                    userWithSession.invalidateSession();
                }
                throw e;
            } finally {
                // Some IDPs don't seem to send updated refresh tokens so keep the existing refresh token.
                if (newTokenResponse != null
                        && newTokenResponse.getRefreshToken() == null
                        && currentTokenResponse.getRefreshToken() != null) {
                    newTokenResponse = newTokenResponse
                            .copy()
                            .refreshToken(currentTokenResponse.getRefreshToken())
                            .refreshTokenExpiresIn(Objects.requireNonNullElseGet(
                                    newTokenResponse.getEffectiveRefreshExpiresIn(),
                                    currentTokenResponse::getEffectiveRefreshExpiresIn))
                            .build();
                }
                fetchTokenResult = new FetchTokenResult(newTokenResponse, jwtClaims);

                LOGGER.debug(LogUtil.message(
                        "New token expiry max age: {}, refresh token expiry max age: {}",
                        NullSafe.toString(newTokenResponse, TokenResponse::getExpiresIn, Duration::ofSeconds),
                        NullSafe.toString(newTokenResponse,
                                TokenResponse::getEffectiveRefreshExpiresIn,
                                Duration::ofSeconds)));
            }
        }
        return fetchTokenResult;
    }

//    private void addUserIdentityToRefreshQueueIfRequired(final UserIdentity userIdentity) {
//        if (userIdentity instanceof final AbstractTokenUserIdentity tokenUserIdentity) {
//            LOGGER.debug(() -> LogUtil.message("Adding identity to the refresh queue, userIdentity: {}, delay: {}",
//                    userIdentity, Duration.ofMillis(tokenUserIdentity.getDelay(TimeUnit.MILLISECONDS))));
//
//            // TODO: 02/03/2023 check not needed if we are dealing with a HasRefreshableToken
//            if (tokenUserIdentity.hasRefreshToken()
//                    || tokenUserIdentity instanceof ServiceUserIdentity) {
//                refreshTokensDelayQueue.add(tokenUserIdentity);
//            } else {
//                LOGGER.warn("Unable to refresh userIdentity due to lack of refresh token {}", tokenUserIdentity);
//            }
//        }
//    }

    protected void addTokenToRefreshQueue(final Refreshable refreshable) {
        if (refreshable != null) {
            LOGGER.debug(() -> LogUtil.message(
                    "Adding {} to the refresh queue, token: {}, delay: {}",
                    refreshable,
                    Duration.ofMillis(refreshable.getDelay(TimeUnit.MILLISECONDS))));
            updatableTokensDelayQueue.add(refreshable);
        }
    }

    private FetchTokenResult refreshTokens(final TokenResponse existingTokenResponse) {

        final String refreshToken = NullSafe.requireNonNull(
                existingTokenResponse,
                TokenResponse::getRefreshToken,
                () -> "Unable to refresh token as no existing refresh token is available");

        final OpenIdConfiguration openIdConfiguration = openIdConfigProvider.get();
        final String tokenEndpoint = openIdConfiguration.getTokenEndpoint();

        final TokenResponse newTokenResponse = new OpenIdTokenRequestHelper(
                tokenEndpoint, openIdConfiguration, objectMapper, jerseyClientFactory)
                .withGrantType(OpenId.GRANT_TYPE__REFRESH_TOKEN)
                .withRefreshToken(refreshToken)
                .sendRequest(true);

        final JwtClaims jwtClaims = jwtContextFactory.getJwtContext(newTokenResponse.getIdToken())
                .map(JwtContext::getJwtClaims)
                .orElseThrow(() -> new RuntimeException("Unable to extract JWT claims"));

        return new FetchTokenResult(newTokenResponse, jwtClaims);
    }

    private WebTarget createWebTarget(final String endpoint) {
        final Client client = jerseyClientFactory.getNamedClient(JerseyClientName.OPEN_ID);
        return client.target(endpoint);
    }

    private ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private Optional<UserIdentity> createUserIdentity(final HttpServletRequest request,
                                                      final AuthenticationState state,
                                                      final TokenResponse tokenResponse,
                                                      final JwtContext jwtContext) {
        Optional<UserIdentity> optUserIdentity = Optional.empty();
        final JwtClaims jwtClaims = jwtContext.getJwtClaims();

        final String nonce = (String) jwtClaims.getClaimsMap()
                .get(OpenId.NONCE);
        final boolean match = nonce != null && nonce.equals(state.getNonce());
        if (match) {
            optUserIdentity = mapAuthFlowIdentity(jwtContext, request, tokenResponse);
            optUserIdentity
                    .filter(userIdentity -> userIdentity instanceof HasRefreshable<?>)
                    .map(userIdentity -> ((HasRefreshable<?>) userIdentity).getRefreshable())
                    .ifPresent(this::addTokenToRefreshQueue);
        } else {
            // If the nonces don't match we need to redirect to log in again.
            // Maybe the request uses an out-of-date stroomSessionId?
            LOGGER.info(() -> "Received a bad nonce!");
        }

        return optUserIdentity;
    }

    private UserIdentity createServiceUserIdentity() {
//        final OpenIdConfiguration openIdConfiguration = openIdConfigProvider.get();
//        final IdpType idpType = openIdConfiguration.getIdentityProviderType();
//
//        final UserIdentity userIdentity = switch (idpType) {
//            case NO_IDP -> new UserIdentity() {
//                @Override
//                public String getSubjectId() {
//                    return "NO_IDP SERVICE USER";
//                }
//            };
//            case TEST_CREDENTIALS -> createTestServiceUser();
//            case EXTERNAL_IDP -> createExternalServiceUser();
//            default -> throw new RuntimeException(LogUtil.message("{} is not supported for property {}.",
//                    openIdConfiguration.getIdentityProviderType(),
//                    AbstractOpenIdConfig.PROP_NAME_IDP_TYPE));
//        };

        // Delegate creation to an idpType appropriate class
        final UserIdentity userIdentity = serviceUserFactory.createServiceUserIdentity();

        if (userIdentity instanceof final HasRefreshable<?> updatableUserIdentity) {
            NullSafe.consume(updatableUserIdentity.getRefreshable(),
                    this::addTokenToRefreshQueue);
        }
        return userIdentity;
    }

//    private UserIdentity createTestServiceUser() {
//        final UserIdentity serviceUserIdentity = new DefaultOpenIdCredsUserIdentity(
//                defaultOpenIdCredentials.getApiKeyUserEmail(),
//                defaultOpenIdCredentials.getApiKey());
//        LOGGER.info("Created test service user identity {} {}",
//                serviceUserIdentity.getClass().getSimpleName(), serviceUserIdentity);
//        return serviceUserIdentity;
//    }

//    private FetchTokenResult fetchExternalServiceUserToken() {
//        final OpenIdConfiguration openIdConfiguration = openIdConfigProvider.get();
//        final String tokenEndpoint = openIdConfiguration.getTokenEndpoint();
//
//        // Only need the access token for a client_credentials flow
//        final TokenResponse tokenResponse = new OpenIdTokenRequestHelper(
//                tokenEndpoint, openIdConfiguration, objectMapper, jerseyClientFactory)
//                .withGrantType(OpenId.GRANT_TYPE__CLIENT_CREDENTIALS)
//                .addScopes(openIdConfiguration.getClientCredentialsScopes())
//                .sendRequest(false);
//
//        final FetchTokenResult fetchTokenResult = jwtContextFactory.getJwtContext(tokenResponse.getAccessToken())
//                .map(jwtContext ->
//                        new FetchTokenResult(tokenResponse, jwtContext.getJwtClaims()))
//                .orElseThrow(() -> {
//                    throw new RuntimeException("Unable to extract JWT claims for service user");
//                });
//
//        return fetchTokenResult;
//    }

//    private UserIdentity createExternalServiceUser() {
//        // Get the initial token
//        final FetchTokenResult fetchTokenResult = fetchExternalServiceUserToken();
//
//        final JwtClaims jwtClaims = fetchTokenResult.jwtClaims();
//        final UpdatableToken updatableToken = new UpdatableToken(
//                fetchTokenResult.tokenResponse(),
//                jwtClaims,
//                updatableToken2 -> fetchExternalServiceUserToken());
//
//        final UserIdentity serviceUserIdentity = new ServiceUserIdentity(
//                getUniqueIdentity(jwtClaims),
//                getUserDisplayName(jwtClaims).orElse(null),
//                updatableToken);
//
//        // Associate the token with the user it is for
//        updatableToken.setUserIdentity(serviceUserIdentity);
//
//        LOGGER.info("Created external IDP service user identity {} {}",
//                serviceUserIdentity.getClass().getSimpleName(), serviceUserIdentity);
//
//        // Add the identity onto the queue so the tokens get refreshed
//        addTokenToRefreshQueue(updatableToken);
//        return serviceUserIdentity;
//    }

    private void consumeFromRefreshQueue() {
        try {
            // We are called in an infinite while loop so drop out every 2s to allow
            // checking of shutdown state
            final Refreshable refreshable = updatableTokensDelayQueue.poll(
                    2, TimeUnit.SECONDS);

            if (refreshable != null) {
                // It is possible that something else has refreshed the token
                LOGGER.debug("Consuming updatableToken {} from refresh queue (size after: {})",
                        refreshable, updatableTokensDelayQueue.size());
                refreshable.refreshIfRequired();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Refresh delay queue interrupted, assume shutdown is happening so do no more");
        }
    }

//    /**
//     * Gets the unique ID that links the identity on the IDP to the stroom_user.
//     * Maps to the 'name' column in stroom_user table.
//     */
//    protected String getUniqueIdentity(final JwtClaims jwtClaims) {
//        Objects.requireNonNull(jwtClaims);
//        final String uniqueIdentityClaim = openIdConfigProvider.get().getUniqueIdentityClaim();
//        final String id = JwtUtil.getClaimValue(jwtClaims, uniqueIdentityClaim)
//                .orElseThrow(() -> new RuntimeException(LogUtil.message(
//                        "Expecting claims to contain configured uniqueIdentityClaim '{}' " +
//                                "but it is not there, jwtClaims: {}",
//                        uniqueIdentityClaim,
//                        jwtClaims)));
//
//        LOGGER.debug("uniqueIdentityClaim: {}, id: {}", uniqueIdentityClaim, id);
//
//        return id;
//    }
//
//    /**
//     * Gets the unique ID that links the identity on the IDP to the stroom_user.
//     * Maps to the 'name' column in stroom_user table.
//     */
//    protected Optional<String> getUserDisplayName(final JwtClaims jwtClaims) {
//        Objects.requireNonNull(jwtClaims);
//        final String userDisplayNameClaim = openIdConfigProvider.get().getUserDisplayNameClaim();
//        final Optional<String> userDisplayName = JwtUtil.getClaimValue(jwtClaims, userDisplayNameClaim);
//
//        LOGGER.debug("userDisplayNameClaim: {}, userDisplayName: {}", userDisplayNameClaim, userDisplayName);
//
//        return userDisplayName;
//    }

    @Override
    public void start() throws Exception {
        if (refreshExecutorService == null) {
            LOGGER.info("Initialising OIDC token refresh executor");
            refreshExecutorService = Executors.newSingleThreadExecutor();
            refreshExecutorService.submit(() -> {
                while (!Thread.currentThread().isInterrupted()
                        && !isShutdownInProgress.get()) {
                    consumeFromRefreshQueue();
                }
            });
        }
    }

    @Override
    public void stop() throws Exception {
        isShutdownInProgress.set(true);
        if (refreshExecutorService != null) {
            LOGGER.info("Shutting down OIDC token refresh executor");
            refreshExecutorService.shutdownNow();
            // No need to wait for termination the stuff on the queue has no value once
            // we are shutting down
            LOGGER.info("Successfully shut down OIDC token refresh executor");
        }
    }
}
