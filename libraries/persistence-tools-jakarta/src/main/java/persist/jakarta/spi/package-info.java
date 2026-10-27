/**
 * Base service-provider support for the Jakarta Persistence namespace.
 *
 * <p>This package contains the abstract base class shared by all
 * Jakarta-specific ({@code "jakarta"}) persistence service providers.
 * Subclasses inherit the fixed namespace declaration, ensuring they are
 * selected when a JPA&nbsp;3.x specification version is requested.</p>
 *
 * <ul>
 *   <li>{@link persist.jakarta.spi.JakartaPersistenceServiceProvider}
 *       &ndash; abstract base class that returns {@code "jakarta"} from
 *       {@link persist.api.spi.PersistenceServiceProvider#getSupportedNamespace()}.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see persist.api.spi.PersistenceServiceProvider
 */
package persist.jakarta.spi;
