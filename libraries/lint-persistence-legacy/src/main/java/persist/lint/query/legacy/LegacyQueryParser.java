package persist.lint.query.legacy;

import org.hibernate.hql.spi.QueryTranslatorFactory;
import persist.lint.legacy.LegacyNamedObjectParser;
import persist.lint.query.spi.QueryParser;

import java.util.List;

/**
 * {@link QueryParser} implementation for the legacy {@code javax.persistence}
 * namespace.
 *
 * <p>This parser uses Hibernate&nbsp;5 to compile JPQL query strings through
 * the AST-based {@link QueryTranslatorFactory}. The compilation verifies HQL
 * grammar, entity mappings, and property paths entirely offline — no live
 * database connection is required.</p>
 *
 * <p>Instances are created through the {@link #from(List)} factory method.</p>
 *
 * @see LegacyQueryParserFactory
 */
@Deprecated
public class LegacyQueryParser implements QueryParser {

    private final LegacyNamedObjectParser parser;

    private LegacyQueryParser(LegacyNamedObjectParser parser) {
        this.parser = parser;
    }

    /**
     * Creates a new {@code LegacyQueryParser} configured with the supplied
     * entity class names.
     *
     * @param discoveredClassNames fully qualified names of JPA entity classes
     * @return a parser ready to validate JPQL queries
     */
    @Deprecated
    public static LegacyQueryParser from(List<String> discoveredClassNames) {
        return new LegacyQueryParser(LegacyNamedObjectParser.from(discoveredClassNames));
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public void parseQuery(String queryName, String queryString) {
        parser.parseQuery(queryName, queryString);
    }

}
