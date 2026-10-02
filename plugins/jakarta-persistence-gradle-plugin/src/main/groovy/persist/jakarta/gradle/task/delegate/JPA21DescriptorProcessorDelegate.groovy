package persist.jakarta.gradle.task.delegate

class JPA21DescriptorProcessorDelegate extends AbstractDescriptorProcessorDelegate {

    @Override
    List<String> getSupportedVersions() {
        return ["2.1", "2.2"]
    }

}
