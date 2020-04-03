package stroom.authentication.impl.db;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.authentication.dao.JwkDao;
import stroom.authentication.impl.db.jooq.tables.records.JsonWebKeyRecord;
import stroom.db.util.JooqUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static stroom.authentication.impl.db.jooq.tables.JsonWebKey.JSON_WEB_KEY;

@Singleton
class JwkDaoImpl implements JwkDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwkDaoImpl.class);
    private static final int MIN_KEY_AGE_MS = 1000 * 60 * 60 * 24;
    private static final int MAX_KEY_AGE_MS = 1000 * 60 * 60 * 24 * 2;

    private AuthDbConnProvider authDbConnProvider;

    @Inject
    JwkDaoImpl(final AuthDbConnProvider authDbConnProvider) {
        this.authDbConnProvider = authDbConnProvider;
    }

//    /**
//     * This will always return a single public key. If the key doesn't exist it will create it.
//     * If it does exist it will return that.
//     */
//    @Override
//    public PublicJsonWebKey readJwk() {
//        try {
//            JooqUtil.context(authDbConnProvider, context ->
//                    context.selectFrom(Tables.JSON_WEB_KEY).fetchOne());
//            JsonWebKeyRecord existingJwkRecord = JooqUtil.contextResult(authDbConnProvider, context -> context
//                    .selectFrom(Tables.JSON_WEB_KEY).fetchOne());
//
//            if (existingJwkRecord == null) {
//                LOGGER.info("We don't have a saved JWK so we'll create one and save it for use next time.");
//                // We need to set up the jwkId so we know which JWTs were signed by which JWKs.
//                String jwkId = UUID.randomUUID().toString();
//                RsaJsonWebKey jwk = RsaJwkGenerator.generateJwk(2048);
//                jwk.setKeyId(jwkId);
//
//                // Persist the public key
//                JsonWebKeyRecord jwkRecord = new JsonWebKeyRecord();
//                jwkRecord.setKeyid(jwkId);
//                jwkRecord.setJson(jwk.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
//                JooqUtil.context(authDbConnProvider, context -> context.executeInsert(jwkRecord));
//
//                return jwk;
//            } else {
//                LOGGER.info("We do have a saved JWK so we'll re-use it.");
//                PublicJsonWebKey jwk = RsaJsonWebKey.Factory.newPublicJwk(existingJwkRecord.getJson());
//                return jwk;
//            }
//        } catch (JoseException e) {
//            LOGGER.error("Unable to create JWK!", e);
//            throw new RuntimeException(e);
//        }
//    }
//

    /**
     * This will always return a list of public keys creating them if needed.
     */
    @Override
    public List<PublicJsonWebKey> readJwk() {
//        // Delete old records.
//        deleteOldJwkRecords();

        // Add new records.
        addRecords();

        // Fetch back all records.
        final List<JsonWebKeyRecord> list = JooqUtil.contextResult(authDbConnProvider, context ->
                context.selectFrom(JSON_WEB_KEY).fetch());
        return list.stream()
                .map(r -> {
                    try {
                        return RsaJsonWebKey.Factory.newPublicJwk(r.getJson());
                    } catch (JoseException e) {
                        LOGGER.error("Unable to create JWK!", e);
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private void addRecords() {
        // FOr the time being just make sure there is a record.
        final List<JsonWebKeyRecord> list = JooqUtil.contextResult(authDbConnProvider, context ->
                context.selectFrom(JSON_WEB_KEY).fetch());
        if (list.size() < 1) {
            addRecord();
        }


//        final long oldest = System.currentTimeMillis() - MIN_KEY_AGE_MS;
//
//        final List<JsonWebKeyRecord> list = database.selectFrom(JSON_WEB_KEY).fetch();
//        long newest = 0;
//        for (JsonWebKeyRecord record : list) {
//            long createTime = 0;
//            if (record.getCreateTimeMs() != null) {
//                createTime = record.getCreateTimeMs();
//            }
//            newest = Math.max(newest, createTime);
//        }
//
//        if (newest < oldest) {
//            addRecord();
//        }
    }

    private void addRecord() {
        JooqUtil.context(authDbConnProvider, context -> {
            try {
                // We need to set up the jwkId so we know which JWTs were signed by which JWKs.
                String jwkId = UUID.randomUUID().toString();
                RsaJsonWebKey jwk = RsaJwkGenerator.generateJwk(2048);
                jwk.setKeyId(jwkId);
                jwk.setUse("sig");
                jwk.setAlgorithm("RS256");

                // Persist the public key
                JsonWebKeyRecord jwkRecord = new JsonWebKeyRecord();
                jwkRecord.setKeyid(jwkId);
                jwkRecord.setJson(jwk.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));

                // TODO : @66 SET THIS

//                     jwkRecord.setCreateTimeMs(System.currentTimeMillis());
                context.executeInsert(jwkRecord);
            } catch (JoseException e) {
                LOGGER.error("Unable to create JWK!", e);
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteOldJwkRecords() {
        JooqUtil.context(authDbConnProvider, context -> {
            final long oldest = System.currentTimeMillis() - MAX_KEY_AGE_MS;

            final List<JsonWebKeyRecord> list = context.selectFrom(JSON_WEB_KEY).fetch();
            for (JsonWebKeyRecord record : list) {
                long createTime = 0;

                // TODO : @66 SET THIS

//                if (record.getCreateTimeMs() != null) {
//                    createTime = record.getCreateTimeMs();
//                }

                if (createTime < oldest) {
                    context.deleteFrom(JSON_WEB_KEY).where(JSON_WEB_KEY.ID.eq(record.getId()));
                }
            }
        });
    }
}
