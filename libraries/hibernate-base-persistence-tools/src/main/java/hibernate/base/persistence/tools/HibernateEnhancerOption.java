package hibernate.base.persistence.tools;

import java.util.Arrays;

/**
 * Enhancement options that control which bytecode transformations
 * are applied during entity enhancement.
 *
 * <p>Each option corresponds to a specific Hibernate bytecode enhancement feature (such as lazy
 * attribute initialization, inline dirty tracking, bidirectional association management, or extended
 * enhancement) and is associated with a standardized configuration property key.</p>
 *
 * @since 1.0.0
 * @see HibernateUtil#convertSettings(java.util.Map)
 */
public enum HibernateEnhancerOption {

    /**
     * Enable lazy-loading of persistent attributes.
     *
     * <p>When enabled, entity bytecode is modified to intercept field access and load
     * non-collection attributes on demand rather than eagerly at entity load time.</p>
     */
    LAZY_INITIALIZATION("hibernate.enhancement.option.lazy-initialization"),

    /**
     * Enable inline dirty tracking to avoid snapshot comparisons.
     *
     * <p>When enabled, entities track modifications to their own fields directly, eliminating
     * the memory and performance overhead of maintaining snapshot copies during flush operations.</p>
     */
    DIRTY_TRACKING("hibernate.enhancement.option.dirty-tracking"),

    /**
     * Enable automatic bidirectional association management.
     *
     * <p>When enabled, updates to one side of a bidirectional association automatically
     * synchronize the opposite side without requiring manual synchronization logic in domain code.</p>
     */
    ASSOCIATION_MANAGEMENT("hibernate.enhancement.option.association-management"),

    /**
     * Enable extended enhancement beyond the standard JPA model.
     *
     * <p>When enabled, additional bytecode optimizations are applied across inheritance hierarchies
     * and non-entity classes referenced in persistence contexts.</p>
     */
    EXTENDED_ENHANCEMENT("hibernate.enhancement.option.extended-enhancement");

    private final String value;

    HibernateEnhancerOption(String value) {
        this.value = value;
    }

    /**
     * Returns the string configuration property key associated with this option.
     *
     * @return the option key string (e.g.&nbsp;{@code "hibernate.enhancement.option.lazy-initialization"})
     */
    public String value() {
        return value;
    }

    /**
     * Resolves an {@code HibernateEnhancerOption} from its string configuration property key.
     *
     * @param value the property key (e.g.&nbsp;{@code "hibernate.enhancement.option.lazy-initialization"})
     * @return the matching {@code HibernateEnhancerOption}
     * @throws IllegalArgumentException if {@code value} does not match any known option
     */
    public static HibernateEnhancerOption from(String value) {
        return Arrays.stream(HibernateEnhancerOption.values())
                .filter(option -> option.value().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown option: " + value));
    }

}
