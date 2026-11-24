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
 * <p>Implementations analyze compiled entity class bytecode and reflection metadata to report
 * potential design and performance issues without requiring a live database connection or application
 * deployment. Typical checks include:</p>
 * <ul>
 *   <li><strong>Structural checks:</strong> Missing default no-arg constructors, final entity classes
 *       or methods, non-getter/setter method signatures, and missing identifier definitions.</li>
 *   <li><strong>Relationship mapping checks:</strong> Eager fetching on associations (e.g., {@code @OneToMany},
 *       {@code @ManyToMany}, {@code @ManyToOne}, or {@code @OneToOne}), missing {@code mappedBy} attributes
 *       on bidirectional relationships, and unordered {@code @ManyToMany} list collections.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * EntityModelLinterFactory factory = EntityModelLinterFactory.newEntityModelLinterFactory("3.1");
 * EntityModelLinter linter = factory.newEntityModelLinter(
 *     List.of("com.example.model.Order", "com.example.model.OrderItem")
 * );
 *
 * List<LintDiagnostic> diagnostics = linter.lint();
 * for (LintDiagnostic diagnostic : diagnostics) {
 *     if (diagnostic.severity() == LintSeverity.ERROR) {
 *         System.err.println(diagnostic.ruleId() + " [" + diagnostic.className() + "]: " + diagnostic.message());
 *     }
 * }
 * }</pre>
 *
 * @since 4.2.0
 * @see LintDiagnostic
 * @see LintSeverity
 * @see PersistenceLinter
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
