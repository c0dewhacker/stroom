package stroom.security.impl;

import stroom.util.config.annotations.ReadOnly;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.IsStroomConfig;
import stroom.util.shared.validation.ValidationSeverity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.AssertTrue;

@JsonPropertyOrder(alphabetic = true)
public class AuthenticationConfig extends AbstractConfig implements IsStroomConfig {

    public static final String PROP_NAME_AUTHENTICATION_REQUIRED = "authenticationRequired";
    public static final String PROP_NAME_OPENID = "openId";
    public static final String PROP_NAME_PREVENT_LOGIN = "preventLogin";

    private final boolean authenticationRequired;
    private final StroomOpenIdConfig openIdConfig;
    private final boolean preventLogin;

    public AuthenticationConfig() {
        authenticationRequired = true;
        openIdConfig = new StroomOpenIdConfig();
        preventLogin = false;
    }

    @JsonCreator
    public AuthenticationConfig(
            @JsonProperty(PROP_NAME_AUTHENTICATION_REQUIRED) final boolean authenticationRequired,
            @JsonProperty(PROP_NAME_OPENID) final StroomOpenIdConfig openIdConfig,
            @JsonProperty(PROP_NAME_PREVENT_LOGIN) final boolean preventLogin) {

        this.authenticationRequired = authenticationRequired;
        this.openIdConfig = openIdConfig;
        this.preventLogin = preventLogin;
    }

    @ReadOnly
    @JsonProperty(PROP_NAME_AUTHENTICATION_REQUIRED)
    @JsonPropertyDescription("Choose whether Stroom requires authenticated access. " +
            "Only intended for use in development or testing.")
    @AssertTrue(
            message = "All authentication is disabled. This should only be used in development or test environments.",
            payload = ValidationSeverity.Warning.class)
    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    @JsonProperty(PROP_NAME_OPENID)
    public StroomOpenIdConfig getOpenIdConfig() {
        return openIdConfig;
    }

    @JsonPropertyDescription("Prevent new logins to the system. This is useful if the system is scheduled to " +
            "have an outage.")
    @JsonProperty(PROP_NAME_PREVENT_LOGIN)
    public boolean isPreventLogin() {
        return preventLogin;
    }

    @Override
    public String toString() {
        return "AuthenticationConfig{" +
                ", authenticationRequired=" + authenticationRequired +
                ", preventLogin=" + preventLogin +
                '}';
    }
}
