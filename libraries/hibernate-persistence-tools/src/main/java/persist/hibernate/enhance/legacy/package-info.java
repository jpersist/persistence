/**
 * Legacy (javax) Persistence Hibernate bytecode enhancement implementation.
 *
 * <p>This package provides the
 * {@link Enhancer} and
 * {@link EnhancerProvider}
 * implementations that target the {@code javax.persistence} namespace
 * (JPA&nbsp;2.x). It uses the Hibernate&nbsp;5 bytecode enhancement SPI
 * to transform compiled entity classes at build time.</p>
 *
 * <ul>
 *   <li>{@link persist.hibernate.enhance.legacy.LegacyHibernateEnhancer}
 *       &ndash; enhances entity class bytes using Hibernate&nbsp;5,
 *       configured with a set of
 *       {@link Enhancer.Option}
 *       flags.</li>
 *   <li>{@link persist.hibernate.enhance.legacy.LegacyHibernateEnhancerProvider}
 *       &ndash; registered as a {@link java.util.ServiceLoader} provider
 *       for {@link EnhancerProvider}.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see Enhancer
 * @see EnhancerProvider
 */
package persist.hibernate.enhance.legacy;

import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;
