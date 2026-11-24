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

import persistence.tools.LintDiagnostic;
import persistence.tools.LintSeverity;

/**
 * Enumerates standard JPA entity mapping, structural, and performance anti-pattern rules
 * shared across Hibernate reference implementations.
 *
 * <p>Each rule defines a standardized rule identifier (e.g.&nbsp;{@code "JPA001"}),
 * a default {@link LintSeverity}, and a descriptive diagnostic message. Factory helper
 * methods create pre-populated {@link LintDiagnostic} instances targeting class or attribute levels.</p>
 *
 * @since 1.2.0
 * @see LintDiagnostic
 * @see LintSeverity
 * @see AbstractEntityModelLinter
 */
public enum EntityLintRule {

    /**
     * JPA specification requires entity and embeddable classes to have a
     * {@code public} or {@code protected} no-argument constructor.
     */
    MISSING_NO_ARG_CONSTRUCTOR(
        "JPA001",
        LintSeverity.ERROR,
        "Entity class must declare a public or protected no-argument constructor."
    ),

    /**
     * JPA specification forbids entity classes and persistent methods/fields
     * from being declared {@code final} (prevents lazy proxy generation).
     */
    FINAL_ENTITY_CLASS(
        "JPA002",
        LintSeverity.ERROR,
        "Entity class must not be declared final, as it prevents lazy proxy generation."
    ),

    /**
     * Final persistent methods prevent Hibernate from generating runtime proxies
     * for lazy-loaded associations.
     */
    FINAL_PERSISTENT_METHOD(
        "JPA003",
        LintSeverity.ERROR,
        "Persistent getter/setter method must not be declared final."
    ),

    /**
     * Using {@code FetchType.EAGER} on to-many associations ({@code @OneToMany},
     * {@code @ManyToMany}) leads to N+1 queries and Cartesian product issues.
     */
    EAGER_TO_MANY_ASSOCIATION(
        "JPA004",
        LintSeverity.WARNING,
        "To-many association is configured with FetchType.EAGER; prefer FetchType.LAZY to avoid N+1 query issues."
    ),

    /**
     * Using {@code FetchType.EAGER} on to-one associations ({@code @ManyToOne},
     * {@code @OneToOne}) causes unnecessary joins or extra select statements.
     */
    EAGER_TO_ONE_ASSOCIATION(
        "JPA005",
        LintSeverity.INFO,
        "To-one association defaults to or explicitly uses FetchType.EAGER; consider FetchType.LAZY."
    ),

    /**
     * Bidirectional associations should specify {@code mappedBy} on the inverse side
     * to avoid redundant join tables or duplicate foreign key updates.
     */
    MISSING_MAPPED_BY(
        "JPA006",
        LintSeverity.WARNING,
        "Collection association does not specify 'mappedBy', which may cause Hibernate to generate an unnecessary join table."
    ),

    /**
     * Using {@code java.util.List} without {@code @OrderColumn} on {@code @ManyToMany}
     * associations causes Hibernate to delete and re-insert all rows on modification.
     */
    UNORDERED_MANY_TO_MANY_LIST(
        "JPA007",
        LintSeverity.WARNING,
        "@ManyToMany association uses java.util.List without @OrderColumn; prefer java.util.Set for better delete/update performance."
    );

    private final String ruleId;
    private final LintSeverity defaultSeverity;
    private final String defaultMessage;

    EntityLintRule(String ruleId, LintSeverity defaultSeverity, String defaultMessage) {
        this.ruleId = ruleId;
        this.defaultSeverity = defaultSeverity;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Returns the unique identifier for this lint rule (e.g.&nbsp;{@code "JPA001"}).
     *
     * @return the rule identifier string
     */
    public String ruleId() {
        return ruleId;
    }

    /**
     * Returns the default severity level assigned to violations of this rule.
     *
     * @return the default {@link LintSeverity}
     */
    public LintSeverity defaultSeverity() {
        return defaultSeverity;
    }

    /**
     * Returns the default diagnostic explanation message for this rule.
     *
     * @return the default message string
     */
    public String defaultMessage() {
        return defaultMessage;
    }

    /**
     * Creates a class-level {@link LintDiagnostic} for this rule using the default message and severity.
     *
     * @param className the fully-qualified name of the violating entity class
     * @return a new {@link LintDiagnostic} representing the class-level violation
     */
    public LintDiagnostic diagnostic(String className) {
        return new LintDiagnostic(defaultSeverity, ruleId, className, null, defaultMessage);
    }

    /**
     * Creates an attribute-level {@link LintDiagnostic} for this rule using the default message and severity.
     *
     * @param className     the fully-qualified name of the entity class declaring the attribute
     * @param attributeName the name of the violating field or property method
     * @return a new {@link LintDiagnostic} representing the attribute-level violation
     */
    public LintDiagnostic diagnostic(String className, String attributeName) {
        return new LintDiagnostic(defaultSeverity, ruleId, className, attributeName, defaultMessage);
    }

    /**
     * Creates an attribute-level {@link LintDiagnostic} for this rule with a custom message.
     *
     * @param className     the fully-qualified name of the entity class declaring the attribute
     * @param attributeName the name of the violating field or property method
     * @param customMessage the specific diagnostic message describing the violation
     * @return a new {@link LintDiagnostic} representing the attribute-level violation with the custom message
     */
    public LintDiagnostic diagnostic(String className, String attributeName, String customMessage) {
        return new LintDiagnostic(defaultSeverity, ruleId, className, attributeName, customMessage);
    }

}
