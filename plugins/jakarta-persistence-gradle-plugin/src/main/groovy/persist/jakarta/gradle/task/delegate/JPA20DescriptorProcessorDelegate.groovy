package persist.jakarta.gradle.task.delegate

class JPA20DescriptorProcessorDelegate extends AbstractDescriptorProcessorDelegate {

    @Override
    List<String> getSupportedVersions() {
        return ["2.0"]
    }

}
