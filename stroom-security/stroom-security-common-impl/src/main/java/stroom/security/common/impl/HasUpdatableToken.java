package stroom.security.common.impl;

import stroom.util.NullSafe;

import org.jose4j.jwt.JwtClaims;

public interface HasUpdatableToken extends HasJwtClaims {

    /**
     * @return The container for the updatable token(s)
     */
    UpdatableToken getUpdatableToken();

    @Override
    default JwtClaims getJwtClaims() {
        return NullSafe.get(getUpdatableToken(), UpdatableToken::getJwtClaims);
    }
}
