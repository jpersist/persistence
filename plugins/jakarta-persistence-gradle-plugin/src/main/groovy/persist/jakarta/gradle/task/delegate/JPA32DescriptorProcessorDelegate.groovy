package persist.jakarta.gradle.task.delegate

/**
 * Descriptor processor delegate for the JPA 3.2 specification.
 *
 * @since 1.4.0
 * @see AbstractDescriptorProcessorDelegate
 */
class JPA32DescriptorProcessorDelegate extends AbstractDescriptorProcessorDelegate {

    @Override
    List<String> getSupportedVersions() {
        return ["3.2"]
    }

}
