package persist.lint.api.spi;

/**
 * Marker interface for all persistence linting operations.
 *
 * <p>This is the root type in the linter hierarchy. Concrete linter
 * contracts such as {@link NamedObjectParser} and {@link SchemaValidator}
 * extend this interface to declare specific build-time validation
 * capabilities.</p>
 *
 * @since 2.1.0
 * @see NamedObjectParser
 * @see SchemaValidator
 */
public interface PersistenceLinter {
}
