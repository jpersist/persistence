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

package persistence.tools;

/**
 * Immutable diagnostic record representing a single JPA mapping or performance
 * issue detected by an {@link EntityModelLinter}.
 *
 * @param severity      the severity level of the finding (e.g., {@link LintSeverity#ERROR},
 *                      {@link LintSeverity#WARNING}, or {@link LintSeverity#INFO})
 * @param ruleId        the unique identifier of the violated linting rule
 * @param className     the fully qualified name of the inspected entity or embeddable class
 * @param attributeName the name of the offending persistent attribute or field, or {@code null}
 *                      if the finding applies at the class level
 * @param message       a human-readable description of the anti-pattern and suggested remediation
 * @since 4.2.0
 * @see EntityModelLinter
 * @see LintSeverity
 */
public record LintDiagnostic(LintSeverity severity, String ruleId, String className,
                             String attributeName, String message) {
}
