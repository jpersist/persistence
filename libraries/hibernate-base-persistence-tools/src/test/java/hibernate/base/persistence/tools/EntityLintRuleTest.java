/*
 * Copyright 2024-2026 JPersist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hibernate.base.persistence.tools;

import org.junit.jupiter.api.Test;
import persistence.tools.LintDiagnostic;
import persistence.tools.LintSeverity;

import static org.assertj.core.api.Assertions.assertThat;

class EntityLintRuleTest {

    @Test
    void shouldExposeRuleMetadata() {
        assertThat(EntityLintRule.MISSING_NO_ARG_CONSTRUCTOR.ruleId()).isEqualTo("JPA001");
        assertThat(EntityLintRule.MISSING_NO_ARG_CONSTRUCTOR.defaultSeverity()).isEqualTo(LintSeverity.ERROR);
        assertThat(EntityLintRule.MISSING_NO_ARG_CONSTRUCTOR.defaultMessage()).isNotBlank();
    }

    @Test
    void shouldCreateClassLevelDiagnostic() {
        LintDiagnostic diagnostic = EntityLintRule.FINAL_ENTITY_CLASS.diagnostic("com.example.Order");

        assertThat(diagnostic.severity()).isEqualTo(LintSeverity.ERROR);
        assertThat(diagnostic.ruleId()).isEqualTo("JPA002");
        assertThat(diagnostic.className()).isEqualTo("com.example.Order");
        assertThat(diagnostic.attributeName()).isNull();
        assertThat(diagnostic.message()).isEqualTo(EntityLintRule.FINAL_ENTITY_CLASS.defaultMessage());
    }

    @Test
    void shouldCreateAttributeLevelDiagnosticWithDefaultMessage() {
        LintDiagnostic diagnostic = EntityLintRule.EAGER_TO_MANY_ASSOCIATION.diagnostic("com.example.Order", "items");

        assertThat(diagnostic.severity()).isEqualTo(LintSeverity.WARNING);
        assertThat(diagnostic.ruleId()).isEqualTo("JPA004");
        assertThat(diagnostic.className()).isEqualTo("com.example.Order");
        assertThat(diagnostic.attributeName()).isEqualTo("items");
        assertThat(diagnostic.message()).isEqualTo(EntityLintRule.EAGER_TO_MANY_ASSOCIATION.defaultMessage());
    }

    @Test
    void shouldCreateAttributeLevelDiagnosticWithCustomMessage() {
        LintDiagnostic diagnostic = EntityLintRule.EAGER_TO_ONE_ASSOCIATION.diagnostic(
            "com.example.Order",
            "customer",
            "Custom message"
        );

        assertThat(diagnostic.severity()).isEqualTo(LintSeverity.INFO);
        assertThat(diagnostic.ruleId()).isEqualTo("JPA005");
        assertThat(diagnostic.className()).isEqualTo("com.example.Order");
        assertThat(diagnostic.attributeName()).isEqualTo("customer");
        assertThat(diagnostic.message()).isEqualTo("Custom message");
    }
}
