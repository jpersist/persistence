package persist.lint.api.spi;

import persist.lint.api.NamedObjectDescriptor;

import java.util.Set;

/**
 * Service provider interface for parsing and validating named JPA objects
 * (queries and stored procedures) at build time.
 *
 * <p>Implementations compile a JPQL query string or validate a stored
 * procedure declaration against the project's entity metamodel, verifying
 * syntax, entity references, and property paths without requiring a live
 * database connection.</p>
 *
 * @since 2.0.0
 * @see NamedObjectParserFactory
 * @see NamedObjectDescriptor
 */
public interface NamedObjectParser {

    /**
     * Parses and semantically validates the given JPQL query string.
     *
     * <p>If the query contains syntax errors or references unknown entities
     * or properties, the implementation should throw an appropriate runtime
     * exception.</p>
     *
     * @param name        the logical name of the query (e.g.&nbsp;the value of
     *                    {@code @NamedQuery.name})
     * @param queryString the JPQL query string to validate
     * @return a descriptor containing the query metadata and discovered
     *         parameters
     * @throws RuntimeException if the query is syntactically or semantically
     *                          invalid
     */
    NamedObjectDescriptor parseQuery(String name, String queryString);

    /**
     * Validates a named stored procedure declaration.
     *
     * <p>The implementation records the procedure name and its parameter
     * names into a {@link NamedObjectDescriptor} for downstream
     * consumption.</p>
     *
     * @param name           the logical name of the stored procedure
     *                       (e.g.&nbsp;the value of
     *                       {@code @NamedStoredProcedureQuery.name})
     * @param procedureName  the database procedure name
     * @param parameterNames the set of parameter names declared on the
     *                       stored procedure
     * @return a descriptor containing the stored procedure metadata
     */
    NamedObjectDescriptor parseStoredProcedure(String name, String procedureName, Set<String> parameterNames);

}
