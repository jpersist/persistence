package persist.jakarta.gradle.task.delegate

/**
 * Descriptor processor delegate for the JPA 2.0 specification.
 *
 * @since 1.4.0
 * @see AbstractDescriptorProcessorDelegate
 */
class JPA20DescriptorProcessorDelegate extends AbstractDescriptorProcessorDelegate {

    @Override
    List<String> getSupportedVersions() {
        return ["2.0"]
    }

}
