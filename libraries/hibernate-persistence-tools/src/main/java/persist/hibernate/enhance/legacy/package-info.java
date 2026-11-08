/**
 * Legacy (javax) Persistence Hibernate bytecode enhancement implementation.
 *
 * <p>This package provides the
 * {@link persist.hibernate.enhance.spi.HibernateEnhancer} and
 * {@link persist.hibernate.enhance.spi.HibernateEnhancerProvider}
 * implementations that target the {@code javax.persistence} namespace
 * (JPA&nbsp;2.x). It uses the Hibernate&nbsp;5 bytecode enhancement SPI
 * to transform compiled entity classes at build time.</p>
 *
 * <ul>
 *   <li>{@link persist.hibernate.enhance.legacy.LegacyHibernateEnhancer}
 *       &ndash; enhances entity class bytes using Hibernate&nbsp;5,
 *       configured with a set of
 *       {@link persist.hibernate.enhance.spi.HibernateEnhancer.Option}
 *       flags.</li>
 *   <li>{@link persist.hibernate.enhance.legacy.LegacyHibernateEnhancerProvider}
 *       &ndash; registered as a {@link java.util.ServiceLoader} provider
 *       for {@link persist.hibernate.enhance.spi.HibernateEnhancerProvider}.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see persist.hibernate.enhance.spi.HibernateEnhancer
 * @see persist.hibernate.enhance.spi.HibernateEnhancerProvider
 */
package persist.hibernate.enhance.legacy;
