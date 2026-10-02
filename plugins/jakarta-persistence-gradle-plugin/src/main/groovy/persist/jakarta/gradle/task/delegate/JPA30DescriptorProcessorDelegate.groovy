package persist.jakarta.gradle.task.delegate

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
