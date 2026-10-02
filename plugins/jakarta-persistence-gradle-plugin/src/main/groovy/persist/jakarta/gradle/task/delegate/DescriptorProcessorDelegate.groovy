package persist.jakarta.gradle.task.delegate

import persist.jakarta.gradle.extension.PersistenceUnitExtension

/**
 * Strategy interface governing the descriptor generation and merge phases
 * for individual JPA specification eras.
 */
interface DescriptorProcessorDelegate {

    void process(File source, File target, List<String> managedClasses,
                 List<String> resolvedJars, List<PersistenceUnitExtension> configuredUnits,
                 Map<String, String> settings, String xmlVersion)

    /**
     * Returns the list of JPA specification versions supported by this delegate.
     */
    List<String> getSupportedVersions()

}
