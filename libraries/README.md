# 🛠️ Persistence Tools Libraries: Build-Time JPA Validation & Enhancement

![Maven Central](https://img.shields.io/maven-central/v/io.github.jpersist.tools/persistence-tools-api)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries)
![GitHub License](https://img.shields.io/github/license/jpersist/persistence)

The **Persistence Tools Libraries** (group `io.github.jpersist.tools`) provide a **vendor-agnostic API** and **reference implementations** for build-time validation, query parsing, schema verification, and bytecode enhancement of JPA persistence artifacts — without deploying the application or running dynamic bytecode agents.

By decoupling tooling contracts from specific persistence providers, the core API enables consistent static analysis across JPA specification baselines. The libraries ship with complete reference implementations powered by **Hibernate ORM 6** (for Jakarta Persistence / JPA 3.x) and **Hibernate 5** (for legacy Java Persistence / JPA 2.x), backed by a shared base infrastructure module.

---

## 📦 Modules

| Module | Artifact | Version | Description |
|:---|:---|:---|:---|
| **`persistence-tools-api`** | `io.github.jpersist.tools:persistence-tools-api` | `4.0.0` | Defines the vendor-agnostic public contracts and Service Provider Interfaces (SPIs): `PersistenceServiceProvider` (shared root), `PersistenceLinter` / `PersistenceLinterFactory` (linting contracts), `NamedObjectParser` / `NamedObjectParserFactory` (named query & stored procedure validation; `AutoCloseable`), `SchemaValidator` / `SchemaValidatorFactory` (database schema validation), and `Enhancer` / `EnhancerProvider` (bytecode enhancement). Contains no vendor-specific implementation; provider modules are discovered dynamically at runtime via the Java `ServiceLoader` mechanism. |
| **`hibernate-base-persistence-tools`** | `io.github.jpersist.tools:hibernate-base-persistence-tools` | `1.0.0` | Shared base module for Hibernate implementations. Provides common enhancement configuration options (`HibernateEnhancerOption`) and shared reflection, URL, and class-loading utilities (`HibernateUtil`). Transitively exposes `persistence-tools-api`. |
| **`hibernate-orm-persistence-tools`** | `io.github.jpersist.tools:hibernate-orm-persistence-tools` | `2.0.0` | Jakarta Persistence (JPA 3.x / `jakarta.*` namespace) reference implementation. Powered by **Hibernate ORM 6** to compile JPQL queries into Semantic Query Model (SQM) trees, validate persistence schemas against a live database, and enhance entity bytecode. |
| **`hibernate-persistence-tools`** | `io.github.jpersist.tools:hibernate-persistence-tools` | `1.0.0` | Legacy Java Persistence (JPA 2.x / `javax.*` namespace) reference implementation. Powered by **Hibernate 5** to validate JPQL queries using AST query translation, validate database schemas, and enhance entity bytecode. |
| **`persistence-tools-bom`** | `io.github.jpersist.tools:persistence-tools-bom` | `4.0.0` | Bill of Materials (BOM) for convenient, aligned dependency management across all Persistence Tools modules. |

---

## ⚡ Quick Start

### 1. Add the BOM and dependencies

#### Jakarta Persistence (JPA 3.x)

```groovy
dependencies {
    implementation platform('io.github.jpersist.tools:persistence-tools-bom:4.0.0')

    // Vendor-agnostic Persistence Tools API
    implementation 'io.github.jpersist.tools:persistence-tools-api'

    // Hibernate ORM 6 reference implementation provider
    runtimeOnly 'io.github.jpersist.tools:hibernate-orm-persistence-tools'
}
```

#### Legacy Persistence (JPA 2.x)

```groovy
dependencies {
    implementation platform('io.github.jpersist.tools:persistence-tools-bom:4.0.0')

    // Vendor-agnostic Persistence Tools API
    implementation 'io.github.jpersist.tools:persistence-tools-api'

    // Hibernate 5 reference implementation provider
    runtimeOnly 'io.github.jpersist.tools:hibernate-persistence-tools'
}
```

### 2. Enhance entity bytecode

Use `EnhancerProvider.getEnhancerProvider(String)` to discover the provider for a given JPA specification version (e.g. `"3.1"`), then obtain an `Enhancer` to transform compiled entity classes at build time:

```java
import hibernate.base.persistence.tools.HibernateEnhancerOption;
import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;

EnhancerProvider provider = EnhancerProvider.getEnhancerProvider("3.1");

Enhancer enhancer = provider.getEnhancer(
    classpathFiles,
    Map.of(
        HibernateEnhancerOption.LAZY_INITIALIZATION.value(), true,
        HibernateEnhancerOption.DIRTY_TRACKING.value(), true
    )
);

byte[] originalBytes = Files.readAllBytes(classFile);
byte[] enhancedBytes = enhancer.enhance("com.example.Order", originalBytes);
Files.write(classFile, enhancedBytes);
```

### 3. Validate named queries & stored procedures

Use `NamedObjectParserFactory.newNamedObjectParserFactory(String)` to resolve the factory matching the JPA specification version, then create a `NamedObjectParser` to validate named queries and stored procedures against entity models:

```java
import persistence.tools.NamedObjectDescriptor;
import persistence.tools.NamedObjectParser;
import persistence.tools.spi.NamedObjectParserFactory;

NamedObjectParserFactory factory = NamedObjectParserFactory.newNamedObjectParserFactory("3.1");

try (NamedObjectParser parser = factory.newNamedObjectParser(entityClassNames)) {
    // Validate a named JPQL query
    NamedObjectDescriptor queryDescriptor = parser.parseQuery(
        "Order.findByStatus",
        "SELECT o FROM Order o WHERE o.status = :status");

    // Validate a named stored procedure declaration
    NamedObjectDescriptor procDescriptor = parser.parseStoredProcedure(
        "Order.archiveOldOrders",
        "sp_archive_orders",
        Set.of("cutoffDate", "archiveTable"));
}
```

> **Note:** `NamedObjectParser` implements `AutoCloseable`. Always use try-with-resources to ensure underlying provider resources and registries are properly released.

### 4. Validate persistence schema against a live database

Use `SchemaValidatorFactory.newSchemaValidatorFactory(String)` to obtain the factory, then create a `SchemaValidator` to compare the entity metamodel against a live database schema:

```java
import persistence.tools.SchemaValidator;
import persistence.tools.spi.SchemaValidatorFactory;

SchemaValidatorFactory factory = SchemaValidatorFactory.newSchemaValidatorFactory("3.1");
SchemaValidator validator = factory.newSchemaValidator(entityClassNames);

validator.validateSchema(Map.of(
    "driver",   "org.postgresql.Driver",
    "url",      "jdbc:postgresql://localhost:5432/mydb",
    "username", "sa",
    "password", "secret"
));
```

All factory methods accept JPA version strings (`"2.0"` – `"2.2"` for legacy `javax`, `"3.0"` – `"3.2"` for `jakarta`) and resolve the matching registered provider via Java's `ServiceLoader` API. Overloaded factory methods accepting explicit `ClassLoader` and `ModuleLayer` parameters are available for advanced and custom modular runtime environments.

> **Tip:** You can also use the generic entry point `PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, factoryClass)` to discover any `PersistenceLinterFactory` subtype by version.

---

## 🧩 Java Platform Module System (JPMS) Integration

All **Persistence Tools Libraries** provide native support for the Java Platform Module System (JPMS) with explicit `module-info.java` descriptors, ensuring strong encapsulation and compile-time boundary enforcement.

### 1. Module Identifiers

| Project / Artifact | JPMS Module Name |
|:---|:---|
| `persistence-tools-api` | `io.github.jpersist.tools.api` |
| `hibernate-base-persistence-tools` | `io.github.jpersist.tools.hibernate.base` |
| `hibernate-orm-persistence-tools` | `io.github.jpersist.tools.hibernate.orm` |
| `hibernate-persistence-tools` | `io.github.jpersist.tools.hibernate` |

When building a modular application or tool, declare a dependency on the core API module:

```java
module com.example.myapp {
    requires io.github.jpersist.tools.api;
}
```

*Concrete provider implementations are fully decoupled and automatically discovered at runtime via `ServiceLoader`.*

### 2. Deep Reflection Boundaries (`opens` Directives)

Because Hibernate inspects entity fields and properties using deep reflection during validation and enhancement, entity packages in modular applications must be opened to the active provider module:

#### For Jakarta Persistence (JPA 3.x / Hibernate 6)
```java
module com.example.myapp {
    requires io.github.jpersist.tools.api;

    // Open entity packages to allow Hibernate ORM 6 reflective metadata access
    opens com.example.myapp.domain to io.github.jpersist.tools.hibernate.orm;
}
```

#### For Legacy Persistence (JPA 2.x / Hibernate 5)
```java
module com.example.myapp {
    requires io.github.jpersist.tools.api;

    // Open entity packages to allow Hibernate 5 reflective metadata access
    opens com.example.myapp.domain to io.github.jpersist.tools.hibernate;
}
```

---

## 🏗️ Architecture

```
persistence-tools-api  (Vendor-Agnostic SPI & Public Contracts)
 ├── persistence.tools
 │    ├── Enhancer                         — bytecode enhancement contract
 │    ├── NamedObjectParser                 — named query & stored procedure parser (AutoCloseable)
 │    ├── NamedObjectDescriptor             — immutable metadata result for parsed objects
 │    ├── NamedObjectType                   — QUERY or STORED_PROCEDURE
 │    ├── SchemaValidator                   — database schema validation contract
 │    └── PersistenceLinter                 — root marker interface for linting operations
 └── persistence.tools.spi
      ├── PersistenceServiceProvider        — root SPI; version-to-namespace mapping & ServiceLoader discovery
      ├── PersistenceLinterFactory          — generic base linter factory
      ├── NamedObjectParserFactory          — factory SPI for NamedObjectParser
      ├── SchemaValidatorFactory            — factory SPI for SchemaValidator
      └── EnhancerProvider                  — factory SPI for Enhancer

hibernate-base-persistence-tools  (Shared Hibernate Infrastructure)
 └── hibernate.base.persistence.tools
      ├── HibernateEnhancerOption           — standardized enhancement configuration option keys
      └── HibernateUtil                     — settings conversion, URL & ClassLoader utilities

hibernate-orm-persistence-tools  (Reference Implementation — "jakarta" namespace / Hibernate ORM 6)
 ├── hibernate.jakarta.persistence.tools.provider
 │    ├── HibernatePersistenceServiceProvider
 │    ├── HibernatePersistenceLinterFactory
 │    ├── HibernateNamedObjectParserFactory
 │    ├── HibernateSchemaValidatorFactory
 │    └── HibernateEnhancerProvider
 └── hibernate.jakarta.persistence.tools
      ├── HibernateNamedObjectParser        — SQM-based query validation
      ├── HibernateSchemaValidator          — Hibernate 6 schema validation
      └── HibernateEnhancer                 — Hibernate 6 bytecode enhancement

hibernate-persistence-tools  (Reference Implementation — "javax" namespace / Hibernate 5)
 ├── hibernate.javax.persistence.tools.provider
 │    ├── HibernatePersistenceServiceProvider
 │    ├── HibernatePersistenceLinterFactory
 │    ├── HibernateNamedObjectParserFactory
 │    ├── HibernateSchemaValidatorFactory
 │    └── HibernateEnhancerProvider
 └── hibernate.javax.persistence.tools
      ├── HibernateNamedObjectParser        — AST-based query validation
      ├── HibernateSchemaValidator          — Hibernate 5 schema validation
      └── HibernateEnhancer                 — Hibernate 5 bytecode enhancement

persistence-tools-bom  (Bill of Materials)
 └── Aligns versions of all API, base, and Hibernate provider modules
```

---

## 📖 API Reference

Browse the full API documentation online:

- **[Core API Reference (Vendor-Agnostic)](https://jpersist.github.io/persistence/tools/specifications/api/latest/)** — Native JPMS modular documentation for `persistence-tools-api` (`io.github.jpersist.tools.api`).
- **[Hibernate ORM 6 Suite (Jakarta Persistence)](https://jpersist.github.io/persistence/tools/hibernate-orm/api/latest/)** — Native JPMS modular documentation for `hibernate-orm-persistence-tools` (`io.github.jpersist.tools.hibernate.orm`).
- **[Hibernate 5 Suite (Legacy Persistence)](https://jpersist.github.io/persistence/tools/hibernate/api/latest/)** — Native JPMS modular documentation for `hibernate-persistence-tools` (`io.github.jpersist.tools.hibernate`).

To generate the documentation locally, execute the Gradle tasks from the project root or the `libraries` directory:

```bash
# Generate all documentation variants
./gradlew :libraries:aggregateAllJavadocs
```

Or generate individual documentation suites independently:

```bash
# Core vendor-agnostic API Javadoc (build/docs/javadoc/specifications/)
./gradlew :libraries:aggregateApiJavadoc

# Hibernate ORM 6 Jakarta reference implementation Javadoc (build/docs/javadoc/hibernate-orm/)
./gradlew :libraries:aggregateModernHibernateJavadoc

# Hibernate 5 Legacy reference implementation Javadoc (build/docs/javadoc/hibernate/)
./gradlew :libraries:aggregateLegacyHibernateJavadoc
```

---

## 📌 Requirements

- **Java:** JDK 17 or higher
- **Gradle:** 7.4+ or 8.x+ / 9.x+

## 📄 License

Distributed under the Apache License 2.0. See the [LICENSE](../LICENSE.txt) file for more information.
