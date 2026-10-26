package persist.lint.api;

/**
 * Enumerates the kinds of named objects that can be validated at build time.
 *
 * @since 2.0.0
 * @see NamedObjectDescriptor
 */
public enum NamedObjectType {

    /**
     * A named JPQL query (e.g.&nbsp;declared with {@code @NamedQuery}).
     */
    QUERY,

    /**
     * A named stored procedure (e.g.&nbsp;declared with
     * {@code @NamedStoredProcedureQuery}).
     */
    STORED_PROCEDURE

}
