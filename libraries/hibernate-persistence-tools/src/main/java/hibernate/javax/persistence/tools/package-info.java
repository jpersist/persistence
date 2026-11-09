/**
 * Legacy {@code javax.persistence} tool implementations backed by Hibernate&nbsp;5.
 *
 * <p>This package provides bytecode enhancement, AST query parsing, and database schema
 * validation components targeting the legacy {@code javax.persistence} namespace (JPA&nbsp;2.x):</p>
 *
 * <ul>
 *   <li>{@link hibernate.javax.persistence.tools.HibernateEnhancer} &ndash; transforms
 *       compiled entity class bytes using Hibernate&nbsp;5 bytecode enhancement.</li>
 *   <li>{@link hibernate.javax.persistence.tools.HibernateNamedObjectParser} &ndash; compiles
 *       and validates named JPQL queries into AST trees offline.</li>
 *   <li>{@link hibernate.javax.persistence.tools.HibernateSchemaValidator} &ndash; validates
 *       the JPA entity metamodel against a live database schema.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see persistence.tools
 * @see hibernate.javax.persistence.tools.provider.HibernatePersistenceServiceProvider
 */
package hibernate.javax.persistence.tools;
