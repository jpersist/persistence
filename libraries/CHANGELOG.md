# 📋 Changelog

All notable changes to the **Persistence Tools Libraries** are documented in this file.

## 4.2.0 - 2026-11-24

### 📦 Released Module Versions

| Module | Artifact | Version |
| :--- | :--- | :--- |
| **`persistence-tools-api`** | `io.github.jpersist.tools:persistence-tools-api` | `4.2.0` |
| **`hibernate-base-persistence-tools`** | `io.github.jpersist.tools:hibernate-base-persistence-tools` | `1.2.0` |
| **`hibernate-orm-persistence-tools`** | `io.github.jpersist.tools:hibernate-orm-persistence-tools` | `2.2.0` |
| **`hibernate-persistence-tools`** | `io.github.jpersist.tools:hibernate-persistence-tools` | `1.2.0` |
| **`persistence-tools-bom`** | `io.github.jpersist.tools:persistence-tools-bom` | `4.2.0` |

### 🚀 New Features
* **Offline DDL Export SPI (`persistence-tools-api`):** Introduced `SchemaExporter` (supporting `CREATE`, `DROP`, and `BOTH` actions to `File`, `OutputStream`, and `Writer` targets) and `SchemaExporterFactory` extending `PersistenceServiceProvider` with `ClassLoader` and `ModuleLayer` `ServiceLoader` discovery overloads ([#168](https://github.com/jpersist/persistence/issues/168)).
* **Entity Model Linter SPI (`persistence-tools-api`):** Introduced `EntityModelLinter`, `LintDiagnostic`, `LintSeverity` (`ERROR`, `WARNING`, `INFO`), and `EntityModelLinterFactory` extending `PersistenceLinterFactory` ([#169](https://github.com/jpersist/persistence/issues/169)).
* **Shared Entity Linting & DDL Utilities (`hibernate-base-persistence-tools`):** Added `EntityLintRule` (`JPA001`–`JPA007`), `AbstractEntityModelLinter` for shared reflection-based checks, and offline DDL defaults/writer helpers in `HibernateUtil` ([#170](https://github.com/jpersist/persistence/issues/170)).
* **Hibernate 5 & ORM 6 Implementations (`hibernate-persistence-tools`, `hibernate-orm-persistence-tools`):** Implemented `HibernateSchemaExporter` and `HibernateEntityModelLinter` across both `javax.persistence` (JPA 2.x) and `jakarta.persistence` (JPA 3.x) providers, registered via JPMS `provides` directives and `META-INF/services/` ([#170](https://github.com/jpersist/persistence/issues/170)).

## 4.1.0 - 2026-11-17

### 📌 Released Module Versions

| Module | Artifact | Version |
| :--- | :--- | :--- |
| **`persistence-tools-api`** | `io.github.jpersist.tools:persistence-tools-api` | `4.1.0` |
| **`hibernate-base-persistence-tools`** | `io.github.jpersist.tools:hibernate-base-persistence-tools` | `1.1.0` |
| **`hibernate-orm-persistence-tools`** | `io.github.jpersist.tools:hibernate-orm-persistence-tools` | `2.1.0` |
| **`hibernate-persistence-tools`** | `io.github.jpersist.tools:hibernate-persistence-tools` | `1.1.0` |
| **`persistence-tools-bom`** | `io.github.jpersist.tools:persistence-tools-bom` | `4.1.0` |

### 🚀 New Features
* **Vendor-Agnostic SPI Overloads (`persistence-tools-api`):** Added `newNamedObjectParser(List<String>, Map<String, Object>)` to `NamedObjectParserFactory` and `newSchemaValidator(List<String>, Map<String, Object>)` to `SchemaValidatorFactory` with backwards-compatible `default` delegations ([#154](https://github.com/jpersist/persistence/issues/154)).
* **Three-Tier Settings Merging (`hibernate-base-persistence-tools`):** Added `HibernateUtil.mergeSettings(...)` to merge predefined defaults, consumer overrides, and enforced safety invariants, alongside `HibernateUtil.toConnectionSettings(...)` for mapping simplified JDBC properties ([#155](https://github.com/jpersist/persistence/issues/155)).
* **Offline No-Op Connection Providers (`hibernate-persistence-tools`, `hibernate-orm-persistence-tools`):** Added encapsulated `NoOpConnectionProvider` implementations selectively opened to `org.hibernate.orm.core` to suppress `HHH000181` startup logs during offline query linting ([#156](https://github.com/jpersist/persistence/issues/156), [#157](https://github.com/jpersist/persistence/issues/157)).
* **Custom Settings Support in Providers:** Implemented settings-aware factory overloads in `HibernateNamedObjectParserFactory` and `HibernateSchemaValidatorFactory` across both Hibernate 5 and Hibernate ORM 6, and propagated merged configuration settings into `SchemaManagementTool` and execution options ([#156](https://github.com/jpersist/persistence/issues/156), [#157](https://github.com/jpersist/persistence/issues/157), [#158](https://github.com/jpersist/persistence/issues/158)).
* **Hibernate 6 Offline Dialect Stabilization (`hibernate-orm-persistence-tools`):** Set `jakarta.persistence.database-product-version = "2.2.224"` and `hibernate.jpa.compliance.query = "true"` by default in `HibernateNamedObjectParser` for deterministic offline SQM query compilation ([#157](https://github.com/jpersist/persistence/issues/157)).

### 🐛 Bug Fixes
* **Hibernate 5 Offline Metadata Lookup (`hibernate-persistence-tools`):** Replaced the Hibernate 6-only `hibernate.boot.allow_jdbc_metadata_access` property with `hibernate.temp.use_jdbc_metadata_defaults = "false"` in `HibernateNamedObjectParser`, eliminating `HHH000342` `UnsupportedOperationException` stack traces during offline bootstrap ([#156](https://github.com/jpersist/persistence/issues/156)).

### 🛠️ Build & Maintenance
* **Unified Release & CD Workflow:** Added `gradle-libraries-release-all.yml` and `gradle-libraries-cd-all.yml` to atomically strip `-SNAPSHOT` versions, tag `libs-v${API_VERSION}`, increment patch snapshots, and publish all five library artifacts to Maven Central from the release tag ([#161](https://github.com/jpersist/persistence/issues/161)).
* **Documentation & Javadoc Overviews:** Added **Configuration & Settings Reference** tables to the Javadoc `overview.md` pages for Hibernate 5 and Hibernate ORM 6, added internal `package-info.java` descriptors, and updated `README.md` Quick Start examples.
