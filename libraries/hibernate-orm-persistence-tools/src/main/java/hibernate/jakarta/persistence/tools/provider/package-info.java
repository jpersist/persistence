/**
 * Service Provider Interface (SPI) implementations for Jakarta Persistence via Hibernate ORM&nbsp;6.
 *
 * <p>This package contains the {@link java.util.ServiceLoader} provider and factory implementations
 * supporting the {@code "jakarta"} persistence namespace:</p>
 *
 * <ul>
 *   <li>{@link hibernate.jakarta.persistence.tools.provider.HibernatePersistenceServiceProvider} &ndash;
 *       abstract base provider binding the {@code "jakarta"} namespace.</li>
 *   <li>{@link hibernate.jakarta.persistence.tools.provider.HibernatePersistenceLinterFactory} &ndash;
 *       abstract base linter factory binding the {@code "jakarta"} namespace.</li>
 *   <li>{@link hibernate.jakarta.persistence.tools.provider.HibernateEnhancerProvider} &ndash;
 *       service provider for {@link persistence.tools.spi.EnhancerProvider}.</li>
 *   <li>{@link hibernate.jakarta.persistence.tools.provider.HibernateNamedObjectParserFactory} &ndash;
 *       service provider for {@link persistence.tools.spi.NamedObjectParserFactory}.</li>
 *   <li>{@link hibernate.jakarta.persistence.tools.provider.HibernateSchemaValidatorFactory} &ndash;
 *       service provider for {@link persistence.tools.spi.SchemaValidatorFactory}.</li>
 * </ul>
 *
 * @since 2.0.0
 * @see persistence.tools.spi
 * @see hibernate.jakarta.persistence.tools.HibernateEnhancer
 */
package hibernate.jakarta.persistence.tools.provider;
