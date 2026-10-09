package persist.lint.query.legacy;

import persist.lint.query.spi.QueryParser;
import persist.lint.query.spi.QueryParserFactory;

import java.util.List;

public class LegacyQueryParserFactory implements QueryParserFactory {

    @Override
    public String getSupportedNamespace() {
        return "javax";
    }

    @Override
    public QueryParser newQueryParser(List<String> discoveredClassNames) {
        return LegacyQueryParser.from(discoveredClassNames);
    }

}
