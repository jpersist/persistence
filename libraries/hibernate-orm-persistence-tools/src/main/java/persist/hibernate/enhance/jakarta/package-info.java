/**
 * Jakarta Persistence Hibernate bytecode enhancement implementation.
 *
 * <p>This package provides the
 * {@link Enhancer} and
 * {@link EnhancerProvider}
 * implementations that target the {@code jakarta.persistence} namespace
 * (JPA&nbsp;3.x). It uses the Hibernate ORM&nbsp;6 bytecode enhancement
 * SPI to transform compiled entity classes at build time.</p>
 *
 * <ul>
 *   <li>{@link persist.hibernate.enhance.jakarta.JakartaHibernateEnhancer}
 *       &ndash; enhances entity class bytes using Hibernate&nbsp;6,
 *       configured with a set of
 *       {@link Enhancer.Option}
 *       flags.</li>
 *   <li>{@link persist.hibernate.enhance.jakarta.JakartaHibernateEnhancerProvider}
 *       &ndash; registered as a {@link java.util.ServiceLoader} provider
 *       for {@link EnhancerProvider}.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see Enhancer
 * @see EnhancerProvider
 */
package persist.hibernate.enhance.jakarta;

import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;
