# 🔍 Persistence Tools Libraries: Build-Time JPA Validation & Enhancement

![Maven Central](https://img.shields.io/maven-central/v/io.github.jpersist.tools/persistence-tools-api)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries)
![GitHub License](https://img.shields.io/github/license/jpersist/persistence)

The **Persistence Tools Libraries** (group `io.github.jpersist.tools`) provide build-time validation and bytecode enhancement of JPA persistence artifacts — named JPQL queries, stored procedure declarations, database schemas, and entity classes — without deploying the application. By compiling query strings against the entity metamodel, comparing the metamodel with a live database schema, and enhancing entity bytecode at build time, syntax errors, unknown entity references, invalid property paths, schema mismatches, and missing runtime instrumentation are caught early.

---

## 📦 Modules

| Module                         | Description                                                                                                                                                                                                     |
|:-------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`persistence-tools-api`**     | Defines the Service Provider Interfaces (SPIs): `PersistenceServiceProvider` (shared root), `PersistenceLinter` / `PersistenceLinterFactory` (linting contracts), `NamedObjectParser` / `NamedObjectParserFactory` (query validation), `SchemaValidator` / `SchemaValidatorFactory` (schema validation), and `HibernateEnhancer` / `HibernateEnhancerProvider` (bytecode enhancement). `NamedObjectParser` extends `AutoCloseable`, so instances should be used within a try-with-resources block. Contains no implementation; provider modules are discovered at runtime through the `ServiceLoader` mechanism. |
| **`persistence-tools-jakarta`** | Jakarta Persistence (JPA 3.x) provider. Uses Hibernate ORM 6 to compile JPQL queries into Semantic Query Model (SQM) trees, validate persistence schemas against a live database, and enhance entity bytecode.     |
| **`persistence-tools-legacy`**  | Legacy `javax.persistence` (JPA 2.x) provider. Uses Hibernate 5 and its AST-based query translator to validate JPQL queries and schemas, and to enhance entity bytecode.                                           |
| **`persistence-tools-bom`**     | Bill of Materials (BOM) for convenient dependency management of all Persistence Tools modules.                                                                                                                   |

---

## ⚡ Quick Start

### 1. Add the BOM and the appropriate provider

#### Jakarta Persistence (JPA 3.x)

```groovy
dependencies {
    implementation platform('io.github.jpersist.tools:persistence-tools-bom:3.0.0')
    implementation 'io.github.jpersist.tools:persistence-tools-jakarta'
}
```

#### Legacy Persistence (JPA 2.x)

```groovy
dependencies {
    implementation platform('io.github.jpersist.tools:persistence-tools-bom:3.0.0')
    implementation 'io.github.jpersist.tools:persistence-tools-legacy'
}
```

### 2. Enhance entity bytecode

Use `HibernateEnhancerProvider.getHibernateEnhancerProvider(String)` to obtain the appropriate provider for a given JPA specification version, then create a `HibernateEnhancer` to transform entity classes at build time:

```java
HibernateEnhancerProvider provider =
    HibernateEnhancerProvider.getHibernateEnhancerProvider("3.1");

HibernateEnhancer enhancer = provider.getEnhancer(
    classesDir,
    classpathFiles,
    Map.of(
        HibernateEnhancer.Option.LAZY_INITIALIZATION, true,
        HibernateEnhancer.Option.DIRTY_TRACKING, true));

byte[] enhancedBytes = enhancer.enhance(
    "com.example.Order", originalClassBytes);
```

### 3. Validate named queries & stored procedures

Use `NamedObjectParserFactory.newNamedObjectParserFactory(String)` to obtain the appropriate factory for a given JPA specification version, then create a `NamedObjectParser` to validate named queries and stored procedures:

```java
NamedObjectParserFactory factory = NamedObjectParserFactory.newNamedObjectParserFactory("3.1");

try (NamedObjectParser parser = factory.newNamedObjectParser(entityClassNames)) {
    // Validate a named query
    NamedObjectDescriptor queryDescriptor = parser.parseQuery(
        "Order.findByStatus",
        "SELECT o FROM Order o WHERE o.status = :status");

    // Validate a named stored procedure
    NamedObjectDescriptor procDescriptor = parser.parseStoredProcedure(
        "Order.archiveOldOrders",
        "sp_archive_orders",
        Set.of("cutoffDate", "archiveTable"));
}
```

> **Note:** `NamedObjectParser` implements `AutoCloseable`. Always use try-with-resources to ensure the underlying Hibernate `SessionFactory` and `ServiceRegistry` are properly released.

### 4. Validate persistence schema against a database

Use `SchemaValidatorFactory.newSchemaValidatorFactory(String)` to obtain the appropriate factory, then create a `SchemaValidator` to compare the entity metamodel with a live database schema:

```java
SchemaValidatorFactory factory = SchemaValidatorFactory.newSchemaValidatorFactory("3.1");
SchemaValidator validator = factory.newSchemaValidator(entityClassNames);

validator.validateSchema(Map.of(
    "driver",   "org.postgresql.Driver",
    "url",      "jdbc:postgresql://localhost:5432/mydb",
    "username", "sa",
    "password", "secret"
));
```

Both factory methods accept JPA version strings (`"2.0"` – `"2.2"` for `javax`, `"3.0"` – `"3.2"` for `jakarta`) and resolve the correct provider via the `ServiceLoader` API.

> **Tip:** You can also use the generic entry point `PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, factoryClass)` to discover any `PersistenceLinterFactory` subtype by version.

---

## 🏗️ Architecture

```
persistence-tools-api  (SPI)
 ├── PersistenceServiceProvider        — shared root; version-to-namespace mapping & ServiceLoader discovery
 ├── PersistenceLinter                 — root marker interface for all linting operations
 ├── PersistenceLinterFactory          — base linter factory; generic ServiceLoader discovery by JPA version
 ├── NamedObjectParser                 — parses and validates named queries and stored procedures (AutoCloseable)
 ├── NamedObjectParserFactory          — creates NamedObjectParser instances
 ├── SchemaValidator                   — validates persistence schema against a live database
 ├── SchemaValidatorFactory            — creates SchemaValidator instances
 ├── HibernateEnhancer                 — transforms entity class bytes (lazy-loading, dirty tracking, etc.)
 ├── HibernateEnhancerProvider         — discovers and creates namespace-specific enhancer instances
 ├── NamedObjectDescriptor             — immutable result containing parsed metadata and parameters
 └── NamedObjectType                   — QUERY or STORED_PROCEDURE

persistence-tools-jakarta  (Provider — "jakarta" namespace)
 ├── JakartaPersistenceLinterFactory          — base factory for Jakarta providers
 ├── JakartaNamedObjectParser                 — Hibernate 6 / SQM-based query validation
 ├── JakartaNamedObjectParserFactory          — creates JakartaNamedObjectParser instances
 ├── JakartaSchemaValidator                   — Hibernate 6-based schema validation
 ├── JakartaSchemaValidatorFactory            — creates JakartaSchemaValidator instances
 ├── JakartaHibernateEnhancer                 — Hibernate 6-based bytecode enhancement
 └── JakartaHibernateEnhancerProvider         — creates JakartaHibernateEnhancer instances

persistence-tools-legacy  (Provider — "javax" namespace)
 ├── LegacyPersistenceLinterFactory           — base factory for Legacy providers
 ├── LegacyNamedObjectParser                  — Hibernate 5 / AST-based query validation
 ├── LegacyNamedObjectParserFactory           — creates LegacyNamedObjectParser instances
 ├── LegacySchemaValidator                    — Hibernate 5-based schema validation
 ├── LegacySchemaValidatorFactory             — creates LegacySchemaValidator instances
 ├── LegacyHibernateEnhancer                  — Hibernate 5-based bytecode enhancement
 └── LegacyHibernateEnhancerProvider          — creates LegacyHibernateEnhancer instances

persistence-tools-bom  (BOM)
 └── Aligns versions of all modules above
```

---

## 📖 API Reference

Browse the full Javadoc at [jpersist.github.io/persistence/libraries/api/latest](https://jpersist.github.io/persistence/libraries/api/latest/)

To generate the documentation locally:

```bash
cd libraries
../gradlew aggregateJavadoc
```

The output is written to `build/docs/javadoc/`.

---

## 📌 Requirements

- **Java:** JDK 17 or higher
- **Gradle:** 7.4+ or 8.x+ / 9.x+

## 📄 License

Distributed under the Apache License 2.0. See the [LICENSE](../LICENSE.txt) file for more information.
