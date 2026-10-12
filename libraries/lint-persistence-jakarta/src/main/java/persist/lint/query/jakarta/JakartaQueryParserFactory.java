package persist.lint.query.jakarta;

import persist.lint.query.spi.QueryParser;
import persist.lint.query.spi.QueryParserFactory;

import java.util.List;

/**
 * {@link QueryParserFactory} implementation for the Jakarta Persistence
 * namespace ({@code "jakarta"}).
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader}
 * provider and creates {@link JakartaQueryParser} instances backed by
 * Hibernate ORM&nbsp;6.</p>
 *
 * @since 1.0.0
 * @deprecated Use {@link persist.lint.jakarta.JakartaNamedObjectParserFactory} instead.
 * @see JakartaQueryParser
 */
@Deprecated
public class JakartaQueryParserFactory implements QueryParserFactory {

    /** Creates a new {@code JakartaQueryParserFactory}. */
    public JakartaQueryParserFactory() {}

    /** {@inheritDoc} */
    @Override
    public String getSupportedNamespace() {
        return "jakarta";
    }

    /** {@inheritDoc} */
    @Override
    public QueryParser newQueryParser(List<String> discoveredClassNames) {
        return JakartaQueryParser.from(discoveredClassNames);
    }

}
