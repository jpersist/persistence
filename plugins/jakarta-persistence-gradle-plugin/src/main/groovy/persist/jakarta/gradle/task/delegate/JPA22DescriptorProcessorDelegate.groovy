package persist.jakarta.gradle.task.delegate

/**
 * Descriptor processor delegate for the JPA 2.2 specifications.
 *
 * @since 1.5.0
 * @see AbstractDescriptorProcessorDelegate
 */
class JPA22DescriptorProcessorDelegate extends AbstractDescriptorProcessorDelegate {

    @Override
    List<String> getSupportedVersions() {
        return ["2.2"]
    }

}
