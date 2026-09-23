/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.mailpit;

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves Mailpit SMTP properties from Docker Compose services.
 */
public final class MailpitSmtpComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    public MailpitSmtpComposeTestResourcesProvider() {
        super("mailpit", Set.of(), 1025, List.of("javamail.properties.mail.smtp.host", "javamail.properties.mail.smtp.port", "javamail.properties.mail.smtp.auth", "javamail.properties.mail.smtp.starttls.enable"));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return switch (context.propertyName()) {
            case "javamail.properties.mail.smtp.host" -> context.host();
            case "javamail.properties.mail.smtp.port" -> String.valueOf(context.port());
            case "javamail.properties.mail.smtp.auth", "javamail.properties.mail.smtp.starttls.enable" -> "false";
            default -> null;
        };
    }
}
