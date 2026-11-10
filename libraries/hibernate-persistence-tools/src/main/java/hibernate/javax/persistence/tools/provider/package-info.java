/**
 * Service Provider Interface (SPI) implementations for legacy {@code javax.persistence} via Hibernate&nbsp;5.
 *
 * <p>This package contains the {@link java.util.ServiceLoader} provider and factory implementations
 * supporting the legacy {@code "javax"} persistence namespace:</p>
 *
 * <ul>
 *   <li>{@link hibernate.javax.persistence.tools.provider.HibernatePersistenceServiceProvider} &ndash;
 *       abstract base provider binding the {@code "javax"} namespace.</li>
 *   <li>{@link hibernate.javax.persistence.tools.provider.HibernatePersistenceLinterFactory} &ndash;
 *       abstract base linter factory binding the {@code "javax"} namespace.</li>
 *   <li>{@link hibernate.javax.persistence.tools.provider.HibernateEnhancerProvider} &ndash;
 *       service provider for {@link persistence.tools.spi.EnhancerProvider}.</li>
 *   <li>{@link hibernate.javax.persistence.tools.provider.HibernateNamedObjectParserFactory} &ndash;
 *       service provider for {@link persistence.tools.spi.NamedObjectParserFactory}.</li>
 *   <li>{@link hibernate.javax.persistence.tools.provider.HibernateSchemaValidatorFactory} &ndash;
 *       service provider for {@link persistence.tools.spi.SchemaValidatorFactory}.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see persistence.tools.spi
 * @see hibernate.javax.persistence.tools.HibernateEnhancer
 */
package hibernate.javax.persistence.tools.provider;
