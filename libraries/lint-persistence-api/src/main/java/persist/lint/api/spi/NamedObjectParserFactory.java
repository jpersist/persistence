package persist.lint.api.spi;

import java.util.List;
import java.util.ServiceLoader;

public interface NamedObjectParserFactory {

    String getSupportedNamespace();

    NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames);

    static NamedObjectParserFactory newNamedObjectParserFactory(String jpaVersion) {
        // Resolve target namespace based on the semantic spec version map token
        final String targetNamespace = switch (jpaVersion) {
            case "2.0", "2.1", "2.2" -> "javax";
            case "3.0", "3.1", "3.2" -> "jakarta";
            default -> throw new IllegalArgumentException("Unsupported JPA version: " + jpaVersion);
        };

        // Drive JVM ServiceLoader API to dynamically discover strategies loaded into the isolated classloader
        ServiceLoader<NamedObjectParserFactory> loader = ServiceLoader.load(NamedObjectParserFactory.class);

        return loader.stream()
            .map(ServiceLoader.Provider::get)
            .filter(factory -> factory.getSupportedNamespace().equals(targetNamespace))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to discover a registered NamedObjectParserFactory provider for namespace: " + targetNamespace));
    }

}
