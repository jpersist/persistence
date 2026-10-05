package persist.jakarta.gradle.task.delegate

/**
 * Descriptor processor delegate for the JPA 2.1 specifications.
 *
 * @since 1.4.0
 * @see AbstractDescriptorProcessorDelegate
 */
class JPA21DescriptorProcessorDelegate extends AbstractDescriptorProcessorDelegate {

    @Override
    List<String> getSupportedVersions() {
        return ["2.1"]
    }

}
