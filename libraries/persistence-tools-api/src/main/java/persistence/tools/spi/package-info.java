/**
 * Service Provider Interfaces (SPI) and discovery mechanisms for Persistence Tools.
 *
 * <p>This package defines the SPI abstractions implemented by persistence providers
 * (e.g.&nbsp;Hibernate ORM&nbsp;6 for Jakarta Persistence or Hibernate&nbsp;5 for legacy
 * {@code javax.persistence}). Implementations are discovered at runtime via the
 * {@link java.util.ServiceLoader} API based on the target JPA specification version.</p>
 *
 * <ul>
 *   <li>{@link persistence.tools.spi.PersistenceServiceProvider} &ndash; root interface for
 *       all persistence service providers, providing namespace resolution and version-based lookup.</li>
 *   <li>{@link persistence.tools.spi.PersistenceLinterFactory} &ndash; base factory contract for
 *       discovering persistence linter factories by JPA version.</li>
 *   <li>{@link persistence.tools.spi.NamedObjectParserFactory} &ndash; factory SPI for creating
 *       {@link persistence.tools.NamedObjectParser} instances.</li>
 *   <li>{@link persistence.tools.spi.SchemaValidatorFactory} &ndash; factory SPI for creating
 *       {@link persistence.tools.SchemaValidator} instances.</li>
 *   <li>{@link persistence.tools.spi.EnhancerProvider} &ndash; SPI for creating
 *       {@link persistence.tools.Enhancer} instances.</li>
 * </ul>
 *
 * @since 4.0.0
 * @see persistence.tools
 */
package persistence.tools.spi;
