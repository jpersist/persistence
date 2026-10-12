package persist.lint.legacy;

import persist.lint.api.spi.NamedObjectParser;
import persist.lint.api.spi.NamedObjectParserFactory;

import java.util.List;

public class LegacyNamedObjectParserFactory  implements NamedObjectParserFactory {

    public LegacyNamedObjectParserFactory() {}

    @Override
    public String getSupportedNamespace() {
        return "javax";
    }

    @Override
    public NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames) {
        return LegacyNamedObjectParser.from(discoveredClassNames);
    }

}
