package persist.lint.query.jakarta;

import persist.lint.jakarta.JakartaNamedObjectParser;
import persist.lint.query.spi.QueryParser;

import java.util.List;

/**
 * {@link QueryParser} implementation for the Jakarta Persistence namespace.
 *
 * <p>This parser uses Hibernate ORM&nbsp;6 to compile JPQL query strings
 * into a Semantic Query Model (SQM) tree. The compilation verifies grammar,
 * entity mappings, property paths, and function references entirely offline
 * — no live database connection is required.</p>
 *
 * <p>Instances are created through the {@link #from(List)} factory method.</p>
 *
 * @see JakartaQueryParserFactory
 */
@Deprecated
public class JakartaQueryParser implements QueryParser {

    private final JakartaNamedObjectParser parser;

    private JakartaQueryParser(JakartaNamedObjectParser parser) {
        this.parser = parser;
    }

    /**
     * Creates a new {@code JakartaQueryParser} configured with the supplied
     * entity class names.
     *
     * @param discoveredClassNames fully qualified names of JPA entity classes
     * @return a parser ready to validate JPQL queries
     */
    @Deprecated
    public static JakartaQueryParser from(List<String> discoveredClassNames) {
        return new JakartaQueryParser(JakartaNamedObjectParser.from(discoveredClassNames));
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public void parseQuery(String queryName, String queryString) {
        parser.parseQuery(queryName, queryString);
    }

}
