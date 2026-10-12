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
 * @since 1.0.0
 * @deprecated Use {@link persist.lint.legacy.LegacyNamedObjectParserFactory} instead.
 * @see LegacyQueryParser
 */
@Deprecated
public class LegacyQueryParserFactory implements QueryParserFactory {

    /** Creates a new {@code LegacyQueryParserFactory}. */
    public LegacyQueryParserFactory() {}

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public String getSupportedNamespace() {
        return "javax";
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public QueryParser newQueryParser(List<String> discoveredClassNames) {
        return LegacyQueryParser.from(discoveredClassNames);
    }

}
