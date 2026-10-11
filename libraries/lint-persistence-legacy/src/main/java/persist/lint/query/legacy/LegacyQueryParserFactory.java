package persist.lint.query.legacy;

import persist.lint.query.spi.QueryParser;
import persist.lint.query.spi.QueryParserFactory;

import java.util.List;

/**
 * {@link QueryParserFactory} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader}
 * provider and creates {@link LegacyQueryParser} instances backed by
 * Hibernate&nbsp;5.</p>
 *
 * @see LegacyQueryParser
 */
public class LegacyQueryParserFactory implements QueryParserFactory {

    /** Creates a new {@code LegacyQueryParserFactory}. */
    public LegacyQueryParserFactory() {}

    /** {@inheritDoc} */
    @Override
    public String getSupportedNamespace() {
        return "javax";
    }

    /** {@inheritDoc} */
    @Override
    public QueryParser newQueryParser(List<String> discoveredClassNames) {
        return LegacyQueryParser.from(discoveredClassNames);
    }

}
