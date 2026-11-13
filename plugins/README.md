# 🔌 JPersist Plugins: Zero-Configuration JPA Build Automation

![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.jpersist.jpa)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_plugins&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_plugins)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_plugins&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_plugins)
![GitHub License](https://img.shields.io/github/license/jpersist/persistence)

**JPersist** is a suite of modern, high-performance, and Gradle Configuration Cache-compliant plugins that automate the configuration of enterprise Jakarta Persistence API (JPA) environments. It provides zero-configuration support for the Java Platform Module System (JPMS), Hibernate, EclipseLink, and OpenJPA multi-module builds — including descriptor generation/merging, schema validation, metamodel generation, bytecode enhancement/weaving, JPQL query linting, GraalVM native-image metadata generation, and consolidated build reporting.

---

## 📦 Plugins

| Plugin ID                                      | Display Name                              | Description                                                                                                                                                                                                                               |
|:-----------------------------------------------|:------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`io.github.jpersist.jpa`**                   | Jakarta Persistence Plugin                | Generates or merges `persistence.xml` descriptors, validates persistence schema, generates JPA GraalVM native-image metadata (`reflect-config.json`), and lints `@NamedQuery` / `@NamedStoredProcedureQuery` definitions at compile time. |
| **`io.github.jpersist.hibernate-persistence`** | Hibernate Persistence Aggregate Plugin    | Aggregate plugin providing corporate-standard Hibernate configuration, binding jpamodelgen and bytecode enhancement out-of-the-box.                                                                                                       |
| **`io.github.jpersist.hibernate-jpamodelgen`** | Hibernate JPA Modelgen Processor Plugin   | Modern, cache-compliant plugin to automatically configure the Hibernate static annotation generator.                                                                                                                                      |
| **`io.github.jpersist.hibernate-enhancement`** | Hibernate Bytecode Enhancement Plugin     | High-performance, cache-compliant plugin that executes programmatic compile-time bytecode enhancement for Hibernate entities.                                                                                                             |
| **`io.github.jpersist.eclipse-persistence`**   | EclipseLink Persistence Aggregate Plugin  | Aggregate plugin that automatically applies both the model generator and compile-time static weaving.                                                                                                                                     |
| **`io.github.jpersist.eclipse-jpa-modelgen`**  | EclipseLink JPA Modelgen Processor Plugin | Modern, cache-compliant plugin to automatically configure the EclipseLink Canonical Model Generator.                                                                                                                                      |
| **`io.github.jpersist.eclipse-static-weave`**  | EclipseLink Static Weaving Plugin         | Executes incremental compile-time bytecode weaving for EclipseLink entity performance enhancement.                                                                                                                                        |
| **`io.github.jpersist.openjpa-persistence`**   | OpenJPA Persistence Aggregate Plugin      | Aggregate plugin providing Apache OpenJPA configuration, binding metamodel generation and compile-time PCEnhancer bytecode enhancement out-of-the-box.                                                                                     |
| **`io.github.jpersist.openjpa-metamodelgen`**  | OpenJPA JPA Modelgen Processor Plugin     | Modern, cache-compliant plugin to automatically configure the OpenJPA static canonical metamodel annotation processor.                                                                                                                     |
| **`io.github.jpersist.openjpa-enhancement`**   | OpenJPA Bytecode Enhancement Plugin       | High-performance, cache-compliant plugin that executes compile-time PCEnhancer bytecode enhancement for OpenJPA entities in an isolated JVM process.                                                                                     |

> **Tip:** The provider-specific aggregate plugins (`hibernate-persistence`, `eclipse-persistence`, and `openjpa-persistence`) automatically apply the `io.github.jpersist.jpa` plugin, so you only need to apply one.

---

## ⚡ Quick Start

### 1. Apply the plugin

#### Hibernate

```groovy
plugins {
    id 'io.github.jpersist.hibernate-persistence' version '1.9.0'
}
```

#### EclipseLink

```groovy
plugins {
    id 'io.github.jpersist.eclipse-persistence' version '1.9.0'
}
```

#### OpenJPA

```groovy
plugins {
    id 'io.github.jpersist.openjpa-persistence' version '1.9.0'
}
```

### 2. Add dependencies

Use the `jpa` configuration to declare your JPA provider platform BOM. This centralizes version management across all compilation scopes.

#### Hibernate

```groovy
dependencies {
    jpa platform('org.hibernate.orm:hibernate-platform:6.6.56.Final')

    implementation 'org.hibernate.orm:hibernate-core'
}
```

#### EclipseLink

```groovy
dependencies {
    jpa platform('org.eclipse.persistence:org.eclipse.persistence.parent:4.0.9')

    compileOnly 'jakarta.persistence:jakarta.persistence-api'
    implementation 'org.eclipse.persistence:org.eclipse.persistence.core'
}
```

#### OpenJPA

```groovy
dependencies {
    jpa 'org.apache.openjpa:openjpa:4.0.0'

    compileOnly 'jakarta.persistence:jakarta.persistence-api:3.1.0'
}
```

That's it — no additional XML configuration or annotation processing setup is required. The plugin automatically detects your entity classes and generates the appropriate `persistence.xml` at build time.

---

## 🏗️ Architecture

```
jakarta-persistence-gradle-plugin  (Core)
 └── JakartaPersistencePlugin
      ├── persistence.xml generation & merging
      ├── Schema validation (single & multi-tenant)
      ├── JPQL @NamedQuery / @NamedStoredProcedureQuery lint
      └── GraalVM reflect-config.json generation

hibernate-persistence-gradle-plugin  (Hibernate Provider)
 ├── HibernatePersistencePlugin      — aggregate: applies jpa + jpamodelgen + enhancement
 ├── HibernateJpamodelgenPlugin      — annotation processor configuration
 └── HibernateEnhancementPlugin      — compile-time bytecode enhancement

eclipse-persistence-gradle-plugin  (EclipseLink Provider)
 ├── EclipsePersistencePlugin        — aggregate: applies jpa + jpa-modelgen + static-weave
 ├── EclipseJpaModelgenPlugin        — annotation processor configuration
 └── EclipseStaticWeavePlugin        — compile-time static weaving

openjpa-persistence-gradle-plugin  (OpenJPA Provider)
 ├── OpenJPAPersistencePlugin        — aggregate: applies jpa + metamodelgen + enhancement
 ├── OpenJPAMetaModelGenerationPlugin — annotation processor configuration
 └── OpenJPAEnhancementPlugin        — compile-time PCEnhancer bytecode enhancement

persistence-gradle-plugin-functional-test  (Functional Tests)
 └── Cross-plugin integration tests via Gradle TestKit
```

---

## ☕ Java Platform Module System (JPMS)

JPersist provides zero-configuration support for modular Java applications using the Java Platform Module System (JPMS / `module-info.java`).

When running build-time bytecode transformation and analysis tasks (`hibernateEnhanceClasses`, `validatePersistenceSchema`, and `lintNamedQueries`), JPersist automatically inspects the compiled classes directory of the target source set:

- **Modular Projects (`module-info.class` present):** Tasks execute inside an isolated forked JVM process (`processIsolation`) configured with `--module-path`, `--add-modules`, dynamic `--add-opens` flags granting Hibernate persistence tools reflective access to all module packages, and bootstrap worker bridge files.
- **Non-Modular Projects (traditional classpath):** Tasks execute in-process using fast `classLoaderIsolation`, avoiding process forking overhead.

Persistence service provider interfaces (such as `SchemaValidatorFactory` and `NamedObjectParserFactory`) are automatically resolved via `PersistenceServiceResolver` across both the modular JPMS boot layer (`ModuleLayer.boot()`) and the thread context classloader.

> **Tip:** If a modularized persistence module applies a model generator plugin, it must either add `requires static java.compiler;` in the `module-info.java` or disable adding `@Generated` annotation on generated static metamodel classes using provider Java compiler options.

---

## 📖 User Guide

Browse the full user guide at [jpersist.github.io/persistence/plugins/guide/latest](https://jpersist.github.io/persistence/plugins/guide/latest/)

To generate the documentation locally:

```bash
cd plugins
../gradlew asciidoctor
```

The output is written to `build/docs/asciidoc/`.

---

## 📖 API Reference

Browse the full Groovydoc at [jpersist.github.io/persistence/plugins/api/latest](https://jpersist.github.io/persistence/plugins/api/latest/)

To generate the documentation locally:

```bash
cd plugins
../gradlew groovydocAll
```

The output is written to `build/docs/groovydoc/`.

---

## 📌 Requirements

- **Java:** JDK 17 or higher
- **Gradle:** 7.4+ or 8.x+ / 9.x+

## 📄 License

Distributed under the Apache License 2.0. See the [LICENSE](../LICENSE.txt) file for more information.
