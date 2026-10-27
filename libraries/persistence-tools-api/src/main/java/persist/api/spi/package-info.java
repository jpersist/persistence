/**
 * Root Service Provider Interface (SPI) for persistence tool providers.
 *
 * <p>This package defines the base contract for all persistence service
 * providers. Each provider declares the JPA namespace it supports
 * (e.g.&nbsp;{@code "jakarta"} or {@code "javax"}) and is discovered at
 * runtime through the {@link java.util.ServiceLoader} mechanism.</p>
 *
 * <ul>
 *   <li>{@link persist.api.spi.PersistenceServiceProvider} &ndash; root
 *       interface for all persistence tool providers, providing namespace
 *       resolution and a static helper to locate a provider by JPA
 *       specification version.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see persist.hibernate.enhance.spi
 * @see persist.lint.api.spi
 */
package persist.api.spi;
