module io.github.jpersist.tools.api {

    exports persist.api.spi;
    exports persist.lint.api;
    exports persist.lint.api.spi;
    exports persist.hibernate.enhance.spi;

    uses persist.api.spi.PersistenceServiceProvider;
    uses persist.lint.api.spi.PersistenceLinterFactory;
    uses persist.lint.api.spi.NamedObjectParserFactory;
    uses persist.lint.api.spi.SchemaValidatorFactory;
    uses persist.hibernate.enhance.spi.HibernateEnhancerProvider;

}
