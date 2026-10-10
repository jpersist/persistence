package persist.lint.query.spi;

/**
 * Service provider interface for parsing and validating named JPQL queries
 * at build time.
 *
 * <p>Implementations of this interface compile a JPQL query string against
 * the project's entity metamodel, verifying syntax, entity references, and
 * property paths without requiring a live database connection.</p>
 *
 * @see QueryParserFactory
 */
public interface QueryParser {

    /**
     * Parses and semantically validates the given JPQL query string.
     *
     * <p>If the query contains syntax errors or references unknown entities
     * or properties, the implementation should throw an appropriate runtime
     * exception.</p>
     *
     * @param queryName   the logical name of the query (e.g. the value of
     *                    {@code @NamedQuery.name})
     * @param queryString the JPQL query string to validate
     * @throws RuntimeException if the query is syntactically or semantically
     *                          invalid
     */
    void parseQuery(String queryName, String queryString);

}
