# 📋 Changelog

All notable changes to the **JPersist Gradle Suite Plugins** are documented in this file.

## v1.9.0 - 2026-11-18

### 📌 Released Plugin Version
* **JPersist Gradle Suite Plugins:** `1.9.0` (aligned with `persistence-tools-bom:4.1.0`)

### 🚀 New Features
* **JPMS Module-Path Execution Isolation (`JPMSWorkerSupport`):** Added automatic detection of modular Java projects (`module-info.class`) across `hibernateEnhanceClasses`, `validatePersistenceSchema`, and `lintNamedQueries`. Modular source sets now execute inside an isolated forked JVM process (`processIsolation`) configured with `--module-path`, `--add-modules`, dynamic `--add-opens` directives for Hibernate persistence tools, and bootstrap worker bridges, while non-modular projects retain fast in-process `classLoaderIsolation` ([#150](https://github.com/jpersist/persistence/issues/150)).
* **Custom Provider Settings in Linter & Schema Validation DSLs:** Added `@Input @Optional MapProperty<String, Object> getSettings()` to `LinterExtension` (`persistence.main.linter.settings`) and `ValidationExtension` (`persistence.main.validation.settings`), forwarding vendor-specific properties (such as target SQL dialects, compliance flags, naming strategies, or `hibernate.default_schema`) into `NamedObjectParserFactory` and `SchemaValidatorFactory` ([#163](https://github.com/jpersist/persistence/issues/163)).

### 🐛 Bug Fixes
* **Worker Classpath & Bytecode Filtering:** Filtered non-JPA classes during named query linting and switched worker classpaths to `runtimeClasspath` to prevent dependency resolution conflicts during build-time validation.

### 🔄 Refactoring & Dependency Alignment
* **Persistence Tools `4.1.0` & BOM Alignment:** Upgraded plugin dependencies to `persistence-tools-bom:4.1.0` and `persistence-tools-api:4.1.0`, aligning Hibernate provider artifact coordinates (`hibernate-persistence-tools`, `hibernate-orm-persistence-tools`) across `JakartaPersistencePlugin` ([#148](https://github.com/jpersist/persistence/issues/148), [#163](https://github.com/jpersist/persistence/issues/163)).
* **SPI Worker Actions & Service Resolution:** Migrated worker actions (`LintNamedQueriesWorker`, `SchemaValidationWorker`, `HibernateEnhancementWorker`) to the `persistence.tools` `4.x` SPI contracts and centralized service discovery across JPMS boot layers and context classloaders via `PersistenceServiceResolver` ([#149](https://github.com/jpersist/persistence/issues/149)).

### 🛠️ Build, Tests & Documentation
* **Modular TestKit Functional Suites:** Added Spock functional tests verifying JPMS worker `processIsolation` as well as end-to-end propagation of `linter.settings` and `validation.settings` under Gradle Configuration Cache ([#151](https://github.com/jpersist/persistence/issues/151), [#163](https://github.com/jpersist/persistence/issues/163)).
* **User Guide & Groovydoc Overviews:** Documented JPMS hybrid worker execution and custom provider `settings` properties with Groovy and Kotlin DSL examples in `configuration.adoc`, `jakarta-plugin.adoc`, `README.md`, and `overview.html` ([#151](https://github.com/jpersist/persistence/issues/151), [#163](https://github.com/jpersist/persistence/issues/163)).
