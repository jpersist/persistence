package persist.lint.jakarta;

import persist.lint.api.spi.NamedObjectParser;
import persist.lint.api.spi.NamedObjectParserFactory;

import java.util.List;

/**
 * {@link NamedObjectParserFactory} implementation for the Jakarta Persistence
 * namespace ({@code "jakarta"}).
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader}
 * provider and creates {@link JakartaNamedObjectParser} instances backed by
 * Hibernate ORM&nbsp;6.</p>
 *
 * @since 2.0.0
 * @see JakartaNamedObjectParser
 */
public class JakartaNamedObjectParserFactory implements NamedObjectParserFactory {

    /** Creates a new {@code JakartaNamedObjectParserFactory}. */
    public JakartaNamedObjectParserFactory() {}

    /** {@inheritDoc} */
    @Override
    public String getSupportedNamespace() {
        return "jakarta";
    }

    /** {@inheritDoc} */
    @Override
    public NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames) {
        return JakartaNamedObjectParser.from(discoveredClassNames);
    }

}
