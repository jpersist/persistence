package persist.lint.api;

import java.util.Set;

/**
 * Immutable descriptor returned after a named object (query or stored
 * procedure) has been successfully parsed and validated.
 *
 * <p>A {@code NamedObjectDescriptor} captures the {@linkplain #type() type}
 * of the named object, its {@linkplain #name() logical name}, the original
 * {@linkplain #value() value} (JPQL string or procedure name), and the
 * discovered parameters.</p>
 *
 * <p>Instances are typically obtained through the factory methods
 * {@link #of(String, String, Set, Set)} (for queries) and
 * {@link #of(String, String, Set)} (for stored procedures).</p>
 *
 * @param type                 the kind of named object
 * @param name                 the logical name (e.g.&nbsp;the value of
 *                             {@code @NamedQuery.name})
 * @param value                the JPQL query string or stored procedure name
 * @param namedParameters      the set of named parameters discovered during
 *                             parsing (e.g.&nbsp;{@code :status})
 * @param positionalParameters the set of positional parameters discovered
 *                             during parsing (e.g.&nbsp;{@code ?1})
 * @since 2.0.0
 * @see NamedObjectType
 * @see persist.lint.api.spi.NamedObjectParser
 */
public record NamedObjectDescriptor(NamedObjectType type, String name, String value,
                                    Set<String> namedParameters,
                                    Set<Integer> positionalParameters) {

    /**
     * Creates a descriptor for a named JPQL query.
     *
     * @param name                 the logical query name
     * @param queryString          the JPQL query string
     * @param namedParameters      named parameters found in the query
     * @param positionalParameters positional parameters found in the query
     * @return a new descriptor with type {@link NamedObjectType#QUERY}
     */
    public static NamedObjectDescriptor of(String name, String queryString, Set<String> namedParameters,  Set<Integer> positionalParameters) {
        return new NamedObjectDescriptor(NamedObjectType.QUERY, name, queryString, namedParameters, positionalParameters);
    }

    /**
     * Creates a descriptor for a named stored procedure.
     *
     * @param name           the logical stored procedure name
     * @param procedureName  the database procedure name
     * @param parameterNames the set of parameter names declared on the
     *                       stored procedure
     * @return a new descriptor with type
     *         {@link NamedObjectType#STORED_PROCEDURE}
     */
    public static NamedObjectDescriptor of(String name, String procedureName, Set<String> parameterNames) {
        return new NamedObjectDescriptor(NamedObjectType.STORED_PROCEDURE, name, procedureName, parameterNames, Set.of());
    }

}
