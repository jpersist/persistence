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

import java.util.List;

/**
 * Contract for statically inspecting a JPA entity metamodel to detect mapping,
 * structural, and performance anti-patterns at build time.
 *
 * <p>Implementations analyze compiled entity definitions and report all discovered
 * rule violations as structured {@link LintDiagnostic} instances without requiring
 * a live database connection or application deployment.</p>
 *
 * @since 4.2.0
 * @see LintDiagnostic
 * @see LintSeverity
 * @see persistence.tools.spi.EntityModelLinterFactory
 */
public interface EntityModelLinter extends PersistenceLinter {

    /**
     * Executes static analysis rules across the configured entity classes.
     *
     * @return an immutable list of {@link LintDiagnostic} findings discovered during inspection;
     *         returns an empty list if no anti-patterns are detected
     */
    List<LintDiagnostic> lint();

}
