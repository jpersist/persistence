/**
 * SPI for Hibernate bytecode enhancement of JPA entity classes.
 *
 * <p>This package defines the abstractions used to perform build-time
 * bytecode enhancement on compiled JPA entity classes. The enhancement
 * adds Hibernate-specific instrumentation such as lazy-loading proxies,
 * inline dirty tracking, bi-directional association management, and
 * extended enhancement — eliminating the need for runtime bytecode
 * manipulation.</p>
 *
 * <ul>
 *   <li>{@link persist.hibernate.enhance.spi.HibernateEnhancer} &ndash;
 *       core interface for enhancing entity class bytes, along with
 *       helper methods and the
 *       {@link persist.hibernate.enhance.spi.HibernateEnhancer.Option Option}
 *       enum controlling which transformations are applied.</li>
 *   <li>{@link persist.hibernate.enhance.spi.HibernateEnhancerProvider}
 *       &ndash; service-provider interface for obtaining a
 *       namespace-specific {@code HibernateEnhancer}, discovered via
 *       {@link java.util.ServiceLoader}.</li>
 * </ul>
 *
 * <p>Provider implementations for specific JPA namespaces are shipped in
 * separate modules (e.g.&nbsp;{@code persistence-tools-jakarta} and
 * {@code persistence-tools-legacy}).</p>
 *
 * @since 1.0.0
 * @see persist.api.spi.PersistenceServiceProvider
 */
package persist.hibernate.enhance.spi;
