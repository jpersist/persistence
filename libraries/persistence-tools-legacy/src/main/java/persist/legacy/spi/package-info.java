/**
 * Base service-provider support for the legacy javax Persistence namespace.
 *
 * <p>This package contains the abstract base class shared by all
 * legacy ({@code "javax"}) persistence service providers. Subclasses
 * inherit the fixed namespace declaration, ensuring they are selected
 * when a JPA&nbsp;2.x specification version is requested.</p>
 *
 * <ul>
 *   <li>{@link persist.legacy.spi.LegacyPersistenceServiceProvider}
 *       &ndash; abstract base class that returns {@code "javax"} from
 *       {@link persist.api.spi.PersistenceServiceProvider#getSupportedNamespace()}.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see persist.api.spi.PersistenceServiceProvider
 */
package persist.legacy.spi;
