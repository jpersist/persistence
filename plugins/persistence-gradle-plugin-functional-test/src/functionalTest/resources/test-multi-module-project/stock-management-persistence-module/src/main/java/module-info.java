module com.example.stockmanagement.persistence {
    requires static java.compiler;
    requires transitive java.persistence;

    exports com.example.stockmanagement.model;
}
