# 🔍 Lint Persistence Libraries: Build-Time JPQL Query Validation

![Maven Central](https://img.shields.io/maven-central/v/io.github.jpersist.lint/lint-persistence-api)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence)
![GitHub License](https://img.shields.io/github/license/jpersist/persistence)

The **Lint Persistence Libraries** provide build-time validation of named JPQL queries defined in JPA projects. By compiling query strings against the entity metamodel at build time, syntax errors, unknown entity references, and invalid property paths are caught early — before the application is deployed.

---

## 📦 Modules

| Module                         | Description                                                                                                                                                                                                     |
|:-------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`lint-persistence-api`**     | Defines the Service Provider Interface (SPI): `NamedObjectParser` and `NamedObjectParserFactory`. Contains no implementation; provider modules are discovered at runtime through the `ServiceLoader` mechanism. |
| **`lint-persistence-jakarta`** | Jakarta Persistence (JPA 3.x) provider. Uses Hibernate ORM 6 to compile JPQL queries into Semantic Query Model (SQM) trees.                                                                                     |
| **`lint-persistence-legacy`**  | Legacy `javax.persistence` (JPA 2.x) provider. Uses Hibernate 5 and its AST-based query translator to validate JPQL queries.                                                                                    |
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
NamedObjectParser parser = factory.newNamedObjectParser(entityClassNames);

// Validate a named query
NamedObjectDescriptor queryDescriptor = parser.parseQuery(
    "Order.findByStatus",
    "SELECT o FROM Order o WHERE o.status = :status");

// Validate a named stored procedure
NamedObjectDescriptor procDescriptor = parser.parseStoredProcedure(
    "Order.archiveOldOrders",
    "sp_archive_orders",
    Set.of("cutoffDate", "archiveTable"));
```

The factory method accepts JPA version strings (`"2.0"` – `"2.2"` for `javax`, `"3.0"` – `"3.2"` for `jakarta`) and resolves the correct provider via the `ServiceLoader` API.

---

## 🏗️ Architecture

```
lint-persistence-api  (SPI)
 ├── NamedObjectParser        — parses and validates named queries and stored procedures
 ├── NamedObjectParserFactory — creates NamedObjectParser instances; discovered via ServiceLoader
 ├── NamedObjectDescriptor    — immutable result containing parsed metadata and parameters
 └── NamedObjectType          — QUERY or STORED_PROCEDURE

lint-persistence-jakarta  (Provider)
 ├── JakartaNamedObjectParser        — Hibernate 6 / SQM-based implementation
 └── JakartaNamedObjectParserFactory — registered for the "jakarta" namespace

lint-persistence-legacy  (Provider)
 ├── LegacyNamedObjectParser         — Hibernate 5 / AST-based implementation
 └── LegacyNamedObjectParserFactory  — registered for the "javax" namespace

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
