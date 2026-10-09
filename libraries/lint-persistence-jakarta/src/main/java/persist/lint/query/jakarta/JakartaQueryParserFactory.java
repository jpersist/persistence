package persist.lint.query.jakarta;

import persist.lint.query.spi.QueryParser;
import persist.lint.query.spi.QueryParserFactory;

import java.util.List;

public class JakartaQueryParserFactory implements QueryParserFactory {

    @Override
    public String getSupportedNamespace() {
        return "jakarta";
    }

    @Override
    public QueryParser newQueryParser(List<String> discoveredClassNames) {
        return JakartaQueryParser.from(discoveredClassNames);
    }

}
