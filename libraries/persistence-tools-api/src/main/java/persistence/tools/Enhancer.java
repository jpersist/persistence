package persistence.tools;

import persistence.tools.spi.EnhancerProvider;

/**
 * Public API contract for bytecode enhancement and instrumentation of persistence entity classes.
 *
 * <p>An {@code Enhancer} transforms compiled class bytecode to inject persistence-related
 * capabilities directly into domain models at build time. By instrumenting classes ahead of
 * runtime, applications benefit from optimized persistence operations without incurring the
 * overhead or complexity of dynamic runtime bytecode generation and Java agents.</p>
 *
 * <h2>Common Enhancement Capabilities</h2>
 * <p>Depending on provider configuration and options, bytecode enhancement typically includes:</p>
 * <ul>
 *   <li><strong>Lazy Loading &amp; Field Interception:</strong> Injects interception hooks into
 *       persistent field or property accessors, enabling fine-grained lazy initialization of
 *       attributes and associations without dynamic proxy wrappers.</li>
 *   <li><strong>Inline Dirty Tracking:</strong> Tracks attribute mutations directly within
 *       the entity instance, avoiding expensive snapshot comparisons during persistence context
 *       flush operations.</li>
 *   <li><strong>Bidirectional Association Management:</strong> Automatically synchronizes both sides
 *       of bidirectional relationships when one side is updated.</li>
 *   <li><strong>Optimized State Access:</strong> Provides direct field access and internal state
 *       handling for improved runtime execution performance.</li>
 * </ul>
 *
 * <h2>Build-Time Integration</h2>
 * <p>Enhancement is designed to be integrated into build tools and CI/CD pipelines (such as
 * Gradle, Maven, or IDE compiler tasks) after compilation and prior to packaging:</p>
 * <pre>{@code
 * EnhancerProvider provider = EnhancerProvider.getEnhancerProvider("3.1");
 * Enhancer enhancer = provider.getEnhancer(classpathFiles, enhancementSettings);
 *
 * byte[] originalBytes = Files.readAllBytes(classFile);
 * byte[] enhancedBytes = enhancer.enhance("com.example.Order", originalBytes);
 * Files.write(classFile, enhancedBytes);
 * }</pre>
 *
 * <h2>Provider Independence</h2>
 * <p>This interface serves as a vendor-neutral contract. Concrete implementations and provider-specific
 * transformation logic are encapsulated behind {@link EnhancerProvider} and discovered via the
 * {@link java.util.ServiceLoader} mechanism.</p>
 *
 * @since 4.0.0
 * @see EnhancerProvider
 */
public interface Enhancer {

    /**
     * Transforms the bytecode of the specified class to incorporate persistence enhancements.
     *
     * <p>If the target class is eligible for enhancement and matching configuration options
     * are active, this method returns the modified bytecode array. If the class is ineligible,
     * already enhanced, or no transformations are applicable, the original {@code classBytes}
     * array (or an equivalent representation) is returned unmodified.</p>
     *
     * @param className  the fully qualified binary name of the class (e.g., {@code "com.example.Order"})
     * @param classBytes the original compiled class bytecode
     * @return the enhanced bytecode array, or the original unmodified bytecode if no changes were applied
     * @throws RuntimeException if an unrecoverable error occurs during bytecode parsing or transformation
     */
    byte[] enhance(String className, byte[] classBytes);

}
