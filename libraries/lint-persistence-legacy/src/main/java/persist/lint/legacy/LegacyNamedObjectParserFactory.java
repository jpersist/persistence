package persist.lint.legacy;

import persist.lint.api.spi.NamedObjectParser;
import persist.lint.api.spi.NamedObjectParserFactory;

import java.util.List;

/**
 * {@link NamedObjectParserFactory} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader}
 * provider and creates {@link LegacyNamedObjectParser} instances backed by
 * Hibernate&nbsp;5.</p>
 *
 * @since 2.0.0
 * @see LegacyNamedObjectParser
 */
public class LegacyNamedObjectParserFactory implements NamedObjectParserFactory {

    /** Creates a new {@code LegacyNamedObjectParserFactory}. */
    public LegacyNamedObjectParserFactory() {}

    /** {@inheritDoc} */
    @Override
    public String getSupportedNamespace() {
        return "javax";
    }

    /** {@inheritDoc} */
    @Override
    public NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames) {
        return LegacyNamedObjectParser.from(discoveredClassNames);
    }

}
