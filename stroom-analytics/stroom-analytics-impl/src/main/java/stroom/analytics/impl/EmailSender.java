/*
 *
 *   Copyright 2017 Crown Copyright
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package stroom.analytics.impl;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.config.ServerConfig;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Message;

@Singleton
class EmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSender.class);

    private final Provider<AnalyticsConfig> analyticsConfigProvider;
    private final ObjectMapper objectMapper;

    @Inject
    EmailSender(final Provider<AnalyticsConfig> analyticsConfigProvider) {
        this.analyticsConfigProvider = analyticsConfigProvider;
        this.objectMapper = createMapper(true);
    }

    public void send(final String name, final String emailAddress, final Detection detection) {
        final AnalyticsConfig analyticsConfig = analyticsConfigProvider.get();
        final EmailConfig emailConfig = analyticsConfig.getEmailConfig();
        Preconditions.checkNotNull(emailConfig, "Missing 'email' section in config");
        final SmtpConfig smtpConfig = Preconditions.checkNotNull(emailConfig,
                        "Missing 'email' section in config")
                .getSmtpConfig();

        final ServerConfig serverConfig;
        if (!Strings.isNullOrEmpty(smtpConfig.getUsername()) && !Strings.isNullOrEmpty(smtpConfig.getPassword())) {
            serverConfig = new ServerConfig(
                    smtpConfig.getHost(),
                    smtpConfig.getPort(),
                    smtpConfig.getUsername(),
                    smtpConfig.getPassword());
        } else {
            serverConfig = new ServerConfig(
                    smtpConfig.getHost(),
                    smtpConfig.getPort());
        }

        final TransportStrategy transportStrategy = smtpConfig.getTransportStrategy();

        final Email email = new Email();
        email.setFromAddress(emailConfig.getFromName(), emailConfig.getFromAddress());
        email.setReplyToAddress(emailConfig.getFromName(), emailConfig.getFromAddress());
        email.addRecipient(name, emailAddress, Message.RecipientType.TO);
        email.setSubject(detection.getDetectorName());

        try {
            final String text = objectMapper.writeValueAsString(detection);
            email.setText(text);
        } catch (final JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
        }

        LOGGER.info("Sending reset email to user {} at {}:{}",
                serverConfig.getHost(), serverConfig.getPort(), serverConfig.getUsername());
        new Mailer(serverConfig, transportStrategy).sendMail(email);
    }

    private static ObjectMapper createMapper(final boolean indent) {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, indent);
        mapper.setSerializationInclusion(Include.NON_NULL);

        return mapper;
    }
}
