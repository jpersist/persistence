package persist.legacy.spi;

import persist.api.spi.PersistenceServiceProvider;

/**
 * Base class for legacy {@code javax.persistence} ({@code "javax"} namespace)
 * service providers.
 *
 * <p>Subclasses inherit a fixed {@link #getSupportedNamespace()}
 * implementation that returns {@code "javax"}, ensuring they are
 * selected when a JPA&nbsp;2.x specification version is requested.</p>
 *
 * @since 1.0.0
 * @see PersistenceServiceProvider
 */
public abstract class LegacyPersistenceServiceProvider implements PersistenceServiceProvider {

    /** Creates a new {@code LegacyPersistenceServiceProvider}. */
    protected LegacyPersistenceServiceProvider() {
    }

    @Override
    public String getSupportedNamespace() {
        return "javax";
    }

}
