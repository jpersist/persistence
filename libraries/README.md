# 🔍 Lint Persistence Libraries: Build-Time JPA Validation

![Maven Central](https://img.shields.io/maven-central/v/io.github.jpersist.lint/lint-persistence-api)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries)
![GitHub License](https://img.shields.io/github/license/jpersist/persistence)

The **Lint Persistence Libraries** provide build-time validation of named JPQL queries and persistence schemas defined in JPA projects. By compiling query strings against the entity metamodel and comparing the mapped schema with a live database at build time, syntax errors, unknown entity references, invalid property paths, and schema mismatches are caught early — before the application is deployed.

---

## 📦 Modules

| Module                         | Description                                                                                                                                                                                                     |
|:-------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`lint-persistence-api`**     | Defines the Service Provider Interface (SPI): `PersistenceLinter` / `PersistenceLinterFactory` (root contracts), `NamedObjectParser` / `NamedObjectParserFactory` (query validation), and `SchemaValidator` / `SchemaValidatorFactory` (schema validation). `NamedObjectParser` extends `AutoCloseable`, so instances should be used within a try-with-resources block. Contains no implementation; provider modules are discovered at runtime through the `ServiceLoader` mechanism. |
| **`lint-persistence-jakarta`** | Jakarta Persistence (JPA 3.x) provider. Uses Hibernate ORM 6 to compile JPQL queries into Semantic Query Model (SQM) trees and to validate persistence schemas against a live database.                            |
| **`lint-persistence-legacy`**  | Legacy `javax.persistence` (JPA 2.x) provider. Uses Hibernate 5 and its AST-based query translator to validate JPQL queries and schemas.                                                                          |
| **`lint-persistence-bom`**     | Bill of Materials (BOM) for convenient dependency management of all Lint Persistence modules.                                                                                                                   |

---

## ⚡ Quick Start

### 1. Add the BOM and the appropriate provider

#### Jakarta Persistence (JPA 3.x)

```groovy
dependencies {
    implementation platform('io.github.jpersist.lint:lint-persistence-bom:1.0.0')
    implementation 'io.github.jpersist.lint:lint-persistence-jakarta'
}
```

#### Legacy Persistence (JPA 2.x)

```groovy
dependencies {
    implementation platform('io.github.jpersist.lint:lint-persistence-bom:1.0.0')
    implementation 'io.github.jpersist.lint:lint-persistence-legacy'
}
```

### 2. Validate named queries & stored procedures

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

### 3. Validate persistence schema against a database

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
lint-persistence-api  (SPI)
 ├── PersistenceLinter          — root marker interface for all linting operations
 ├── PersistenceLinterFactory   — base factory; generic ServiceLoader discovery by JPA version
 ├── NamedObjectParser          — parses and validates named queries and stored procedures (AutoCloseable)
 ├── NamedObjectParserFactory   — creates NamedObjectParser instances
 ├── SchemaValidator            — validates persistence schema against a live database
 ├── SchemaValidatorFactory     — creates SchemaValidator instances
 ├── NamedObjectDescriptor      — immutable result containing parsed metadata and parameters
 └── NamedObjectType            — QUERY or STORED_PROCEDURE

lint-persistence-jakarta  (Provider — "jakarta" namespace)
 ├── JakartaPersistenceLinterFactory   — base factory for Jakarta providers
 ├── JakartaNamedObjectParser          — Hibernate 6 / SQM-based query validation
 ├── JakartaNamedObjectParserFactory   — creates JakartaNamedObjectParser instances
 ├── JakartaSchemaValidator            — Hibernate 6-based schema validation
 └── JakartaSchemaValidatorFactory     — creates JakartaSchemaValidator instances

lint-persistence-legacy  (Provider — "javax" namespace)
 ├── LegacyPersistenceLinterFactory    — base factory for Legacy providers
 ├── LegacyNamedObjectParser           — Hibernate 5 / AST-based query validation
 ├── LegacyNamedObjectParserFactory    — creates LegacyNamedObjectParser instances
 ├── LegacySchemaValidator             — Hibernate 5-based schema validation
 └── LegacySchemaValidatorFactory      — creates LegacySchemaValidator instances

lint-persistence-bom  (BOM)
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
