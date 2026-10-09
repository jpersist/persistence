package persist.lint.query.spi;

import java.util.List;
import java.util.ServiceLoader;

public interface QueryParserFactory {

    String getSupportedNamespace();

    QueryParser newQueryParser(List<String> discoveredClassNames);

    static QueryParserFactory newQueryParserFactory(String jpaVersion) {
        // Resolve target namespace based on the semantic spec version map token
        final String targetNamespace = switch (jpaVersion) {
            case "2.0", "2.1", "2.2" -> "javax";
            case "3.0", "3.1", "3.2" -> "jakarta";
            default -> throw new IllegalArgumentException("Unsupported JPA version: " + jpaVersion);
        };

        // Drive JVM ServiceLoader API to dynamically discover strategies loaded into the isolated classloader
        ServiceLoader<QueryParserFactory> loader = ServiceLoader.load(QueryParserFactory.class);

        return loader.stream()
            .map(ServiceLoader.Provider::get)
            .filter(factory -> factory.getSupportedNamespace().equals(targetNamespace))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to discover a registered QueryParserFactory provider for namespace: " + targetNamespace));
    }

}
