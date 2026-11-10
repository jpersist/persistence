/**
 * Jakarta Persistence tool implementations backed by Hibernate ORM&nbsp;6.
 *
 * <p>This package provides bytecode enhancement, semantic query model (SQM) query parsing,
 * and database schema validation components targeting the {@code jakarta.persistence}
 * namespace (JPA&nbsp;3.x):</p>
 *
 * <ul>
 *   <li>{@link hibernate.jakarta.persistence.tools.HibernateEnhancer} &ndash; transforms
 *       compiled entity class bytes using Hibernate ORM&nbsp;6 bytecode enhancement.</li>
 *   <li>{@link hibernate.jakarta.persistence.tools.HibernateNamedObjectParser} &ndash; compiles
 *       and validates named JPQL queries into SQM trees offline.</li>
 *   <li>{@link hibernate.jakarta.persistence.tools.HibernateSchemaValidator} &ndash; validates
 *       the JPA entity metamodel against a live database schema.</li>
 * </ul>
 *
 * @since 2.0.0
 * @see persistence.tools
 * @see hibernate.jakarta.persistence.tools.provider.HibernatePersistenceServiceProvider
 */
package hibernate.jakarta.persistence.tools;
