package persist.lint.query.spi;

public interface QueryParser {

    void parseQuery(String queryName, String queryString);

}
