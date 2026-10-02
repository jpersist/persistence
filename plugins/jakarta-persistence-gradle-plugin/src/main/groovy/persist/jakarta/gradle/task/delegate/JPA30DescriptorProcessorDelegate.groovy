package persist.jakarta.gradle.task.delegate

/**
 * Descriptor processor delegate for the JPA 3.0 and 3.1 specifications.
 *
 * @since 1.4.0
 * @see AbstractDescriptorProcessorDelegate
 */
class JPA30DescriptorProcessorDelegate extends AbstractDescriptorProcessorDelegate {

    /**
     * Broad-casts that this single execution engine strategy completely handles
     * both the 3.0 and 3.1 schema specification rules.
     */
    @Override
    List<String> getSupportedVersions() {
        return ["3.0", "3.1"]
    }

}
