package persist.jakarta.gradle.task.delegate

import persist.jakarta.gradle.extension.PersistenceUnitExtension

/**
 * Strategy interface governing the descriptor generation and merge phases
 * for individual JPA specification versions.
 * <p>
 * Each implementation handles a specific set of JPA specification versions
 * and is resolved at runtime via
 * {@link persist.jakarta.gradle.task.registry.JPAVersionStrategyRegistry}.
 * </p>
 *
 * @since 1.4.0
 * @see AbstractDescriptorProcessorDelegate
 */
interface DescriptorProcessorDelegate {

    /**
     * Processes the persistence descriptor by either merging an existing
     * source file or generating a new one from scratch.
     *
     * @param source          The user-provided source descriptor, or {@code null} for generation mode.
     * @param target          The destination file to write.
     * @param managedClasses  The list of managed JPA class names.
     * @param resolvedJars    The list of resolved JAR file names.
     * @param configuredUnits The persistence-unit extension configurations.
     * @param settings        The XML transformer output property overrides.
     * @param xmlVersion      The JPA specification version for the root element.
     */
    void process(File source, File target, List<String> managedClasses,
                 List<String> resolvedJars, List<PersistenceUnitExtension> configuredUnits,
                 Map<String, String> settings, String xmlVersion)

    /**
     * Returns the list of JPA specification versions supported by this delegate.
     *
     * @return A non-empty list of version strings (e.g. {@code ["3.0", "3.1"]}).
     */
    List<String> getSupportedVersions()

}
