package persist.lint.jakarta;

import persist.lint.api.spi.NamedObjectParser;
import persist.lint.api.spi.NamedObjectParserFactory;

import java.util.List;

public class JakartaNamedObjectParserFactory implements NamedObjectParserFactory {

    public JakartaNamedObjectParserFactory() {}

    @Override
    public String getSupportedNamespace() {
        return "jakarta";
    }

    @Override
    public NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames) {
        return JakartaNamedObjectParser.from(discoveredClassNames);
    }

}
