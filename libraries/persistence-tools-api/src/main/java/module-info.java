module io.github.jpersist.tools.api {

    // 1. Export clean, vendor-agnostic user-facing API contracts
    exports persistence.tools;

    // 2. Export service provider interfaces for reference implementations
    exports persistence.tools.spi;

    // 3. Declare modular ServiceLoader discovery targets across the new SPI layout
    uses persistence.tools.spi.PersistenceServiceProvider;
    uses persistence.tools.spi.PersistenceLinterFactory;
    uses persistence.tools.spi.NamedObjectParserFactory;
    uses persistence.tools.spi.SchemaValidatorFactory;
    uses persistence.tools.spi.EnhancerProvider;

}
