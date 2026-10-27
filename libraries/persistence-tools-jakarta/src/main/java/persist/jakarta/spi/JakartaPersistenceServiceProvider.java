package persist.jakarta.spi;

import persist.api.spi.PersistenceServiceProvider;

/**
 * Base class for Jakarta Persistence ({@code "jakarta"} namespace)
 * service providers.
 *
 * <p>Subclasses inherit a fixed {@link #getSupportedNamespace()}
 * implementation that returns {@code "jakarta"}, ensuring they are
 * selected when a JPA&nbsp;3.x specification version is requested.</p>
 *
 * @since 1.0.0
 * @see PersistenceServiceProvider
 */
public abstract class JakartaPersistenceServiceProvider implements PersistenceServiceProvider {

    /** Creates a new {@code JakartaPersistenceServiceProvider}. */
    protected JakartaPersistenceServiceProvider() {
    }

    @Override
    public String getSupportedNamespace() {
        return "jakarta";
    }

}
