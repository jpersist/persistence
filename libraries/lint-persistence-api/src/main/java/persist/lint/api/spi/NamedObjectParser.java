package persist.lint.api.spi;

import persist.lint.api.NamedObjectDescriptor;

import java.util.Set;

public interface NamedObjectParser {

    NamedObjectDescriptor parseQuery(String name, String queryString);

    NamedObjectDescriptor parseStoredProcedure(String name, String procedureName, Set<String> parameterNames);

}
