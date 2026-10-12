package persist.lint.api;

import java.util.Set;

public record NamedObjectDescriptor(NamedObjectType type, String name, String value,
                                    Set<String> namedParameters,
                                    Set<Integer> positionalParameters) {

    public static NamedObjectDescriptor of(String name, String queryString, Set<String> namedParameters,  Set<Integer> positionalParameters) {
        return new NamedObjectDescriptor(NamedObjectType.QUERY, name, queryString, namedParameters, positionalParameters);
    }

    public static NamedObjectDescriptor of(String name, String procedureName, Set<String> parameterNames) {
        return new NamedObjectDescriptor(NamedObjectType.STORED_PROCEDURE, name, procedureName, parameterNames, Set.of());
    }

}
