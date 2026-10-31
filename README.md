# 📦 JPersist: Zero-Boilerplate JPA Build Automation for Gradle

[![Java Gradle CI](https://github.com/jpersist/persistence/actions/workflows/gradle-ci.yml/badge.svg)](https://github.com/jpersist/persistence/actions/workflows/gradle-ci.yml)
![GitHub Release](https://img.shields.io/github/v/release/jpersist/persistence)
![GitHub License](https://img.shields.io/github/license/jpersist/persistence)

### 📊 Component Status Matrices

| Component | Release Version | Quality Gate | Code Coverage |
| :--- | :--- | :--- | :--- |
| **🔌 Gradle Plugins** | ![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.jpersist.jpa) | [![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_plugins&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_plugins) | [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_plugins&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_plugins) |
| **📦 Core Libraries** | ![Maven Central](https://img.shields.io/maven-central/v/io.github.jpersist.lint/lint-persistence-api) | [![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries) | [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries) |

---

## 🧩 What is JPersist?

**JPersist** is a suite of modern, high-performance, and **Gradle Configuration Cache-compliant** plugins that completely automates the tedious boilerplate configuration of enterprise **Jakarta Persistence API (JPA)** environments.

It handles everything a JPA developer needs at build time — from `persistence.xml` descriptor generation and merge to bytecode enhancement, metamodel generation, schema validation, GraalVM native image metadata, and compile-time named query linting — so you can focus on writing entities and queries instead of fighting build scripts.

---

## 🏆 Why JPersist is the Ultimate JPA Development Tool for Gradle

Standard Gradle setups require verbose XML overrides, break incremental compilation states during bytecode manipulation, and crash with complex classpath decoration proxy loops. **JPersist fixes this permanently.**

| Standard Gradle Setup                 | The JPersist Advantage                                                             |
|:--------------------------------------|:-----------------------------------------------------------------------------------|
| ❌ Manual `<class>` tagging required  | **✅ ASM-powered bytecode scanning** auto-detects entities                         |
| ❌ Hardcoded schema structures        | **✅ Strategy-driven polymorphism** supports JPA 2.0 to 3.2 dynamically            |
| ❌ Rigid monolithic output paths      | **✅ Declarative attributes** route individual JAR locations via the DSL           |
| ❌ Broken workspace sync mappings     | **✅ Automated IDE indexing** hooks into Buildship and IntelliJ out-of-the-box     |
| ❌ Named query errors only at runtime | **✅ Compile-time query linting** catches JPQL/HQL syntax errors before deployment |
| ❌ Manual GraalVM reflection config   | **✅ Automatic `reflect-config.json`** generation for native images                |

### ⚡ Key Features

- **Descriptor Generation & Merging** — Generates or intelligently merges `persistence.xml` descriptors per source set using ASM bytecode scanning for automatic entity class discovery.
- **Hibernate Bytecode Enhancement** — Compile-time enhancement for lazy initialization, dirty tracking, and association management without runtime agents.
- **EclipseLink Static Weaving** — Compile-time static weaving fully isolated from the Gradle daemon classpath.
- **OpenJPA Bytecode Enhancement** *(new in v1.8.0)* — Compile-time PCEnhancer-based bytecode enhancement for OpenJPA entities, forked in an isolated JVM process.
- **JPA Static Metamodel Generation** — Automatic annotation processor setup for type-safe Criteria API queries (Hibernate, EclipseLink, and OpenJPA).
- **Schema Validation** — Validates entity mappings against a database schema at build time using an isolated Gradle Worker process.
- **Multi-Tenant Schema Validation** *(new in v1.7.0)* — Extends schema validation with named tenant profiles, each carrying its own JDBC connection properties for validating against multiple database tenants in a single build.
- **GraalVM Native Image Metadata** — Generates `reflect-config.json` for all discovered JPA entities, enabling ahead-of-time reflection registration. *(v1.8.0: new `reflectConfigFile` property; `outputFile` deprecated)*
- **Consolidated Reporting** *(new in v1.8.0)* — All verification reports (schema validation, query linting) are written to a unified `${buildDir}/reports/persistence/` directory for streamlined CI/CD artifact collection.
- **Lint Named Queries** — Static, compile-time AST verification of `@NamedQuery` and `@NamedStoredProcedureQuery` definitions with JPQL/HQL syntax and parameter naming validation.

---

## 🚀 Getting Started

Apply the plugin matching your JPA provider. The `jpa` dependency configuration centralizes version management via a BOM platform across all standard Java compilation scopes.

### <img src="https://cdn.simpleicons.org/hibernate" alt="Hibernate" height="20"> Hibernate Setup

<details open>
  <summary><b>Groovy DSL</b></summary>
  <br>

```groovy
plugins {
    id 'io.github.jpersist.hibernate-persistence' version '1.8.0'
}

dependencies {
    jpa platform('org.hibernate.orm:hibernate-platform:6.6.56.Final')

    implementation 'org.hibernate.orm:hibernate-core'
}
```
</details>

<details>
  <summary><b>Kotlin DSL</b></summary>
  <br>

```kotlin
plugins {
    id("io.github.jpersist.hibernate-persistence") version "1.8.0"
}

dependencies {
    jpa(platform("org.hibernate.orm:hibernate-platform:6.6.56.Final"))

    implementation("org.hibernate.orm:hibernate-core")
}
```
</details>

### <img src="https://cdn.simpleicons.org/eclipseide" alt="EclipseLink" height="20"> EclipseLink Setup

<details open>
  <summary><b>Groovy DSL</b></summary>
  <br>

```groovy
plugins {
    id 'io.github.jpersist.eclipse-persistence' version '1.8.0'
}

dependencies {
    jpa platform('org.eclipse.persistence:org.eclipse.persistence.parent:4.0.9')

    compileOnly 'jakarta.persistence:jakarta.persistence-api'
    implementation 'org.eclipse.persistence:org.eclipse.persistence.core'
}
```
</details>

<details>
  <summary><b>Kotlin DSL</b></summary>
  <br>

```kotlin
plugins {
    id("io.github.jpersist.eclipse-persistence") version "1.8.0"
}

dependencies {
    jpa(platform("org.eclipse.persistence:org.eclipse.persistence.parent:4.0.9"))

    compileOnly("jakarta.persistence:jakarta.persistence-api")
    implementation("org.eclipse.persistence:org.eclipse.persistence.core")
}
```
</details>

### <img src="https://cdn.simpleicons.org/apache" alt="OpenJPA" height="20"> OpenJPA Setup

<details open>
  <summary><b>Groovy DSL</b></summary>
  <br>

```groovy
plugins {
    id 'io.github.jpersist.openjpa-persistence' version '1.8.0'
}

dependencies {
    jpa 'org.apache.openjpa:openjpa:4.0.0'

    compileOnly 'jakarta.persistence:jakarta.persistence-api:3.1.0'
}
```
</details>

<details>
  <summary><b>Kotlin DSL</b></summary>
  <br>

```kotlin
plugins {
    id("io.github.jpersist.openjpa-persistence") version "1.8.0"
}

dependencies {
    jpa("org.apache.openjpa:openjpa:4.0.0")

    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
```
</details>

That's it — no additional XML configuration or annotation processing setup is required.
The plugin automatically detects your entity classes and generates the appropriate `persistence.xml` at build time.

---

## 🏗️ Repository Structure

| Directory                                     | Description                                                                                                               |
|:----------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------|
| `plugins/jakarta-persistence-gradle-plugin`   | Core plugin — generates/merges `persistence.xml`, validates schemas, generates GraalVM metadata, and lints named queries. |
| `plugins/hibernate-persistence-gradle-plugin` | Hibernate aggregate plugin — metamodel generation and compile-time bytecode enhancement.                                  |
| `plugins/eclipse-persistence-gradle-plugin`   | EclipseLink aggregate plugin — metamodel generation and compile-time static weaving.                                      |
| `plugins/openjpa-persistence-gradle-plugin`    | OpenJPA aggregate plugin — metamodel generation and compile-time PCEnhancer bytecode enhancement.                         |
| `libraries/persistence-tools-api`              | SPI contract and core interfaces for the named query linter (`io.github.jpersist.tools:persistence-tools-api`).           |
| `libraries/persistence-tools-jakarta`          | Jakarta Persistence (3.x) linter implementation (`io.github.jpersist.tools:persistence-tools-jakarta`).                   |
| `libraries/persistence-tools-legacy`           | Legacy JPA (2.x) linter implementation (`io.github.jpersist.tools:persistence-tools-legacy`).                             |
| `libraries/persistence-tools-bom`              | BOM (Bill of Materials) for aligning all persistence-tools library versions.                                               |
| `examples/`                                   | Runnable example projects for Hibernate, EclipseLink, and standalone Jakarta Persistence.                                 |

---

## 📚 Further Information

- 📖 **Libraries API Reference (Javadoc):** [jpersist.github.io/persistence/libraries/api/latest](https://jpersist.github.io/persistence/libraries/api/latest/)
- 📖 **Plugins API Reference (Groovydoc):** [jpersist.github.io/persistence/plugins/api/latest](https://jpersist.github.io/persistence/plugins/api/latest/)
- 📘 **User Guide:** [jpersist.github.io/persistence/plugins/guide/latest](https://jpersist.github.io/persistence/plugins/guide/latest/)
- 🚀 **Examples:** Complete runnable project templates in the [`examples/`](examples/) directory.

---

## ⚙️ Requirements

- **Gradle:** 7.4+ or 8.x+ / 9.x+
- **Java:** JDK 17 or higher (required by modern Jakarta specifications)

## 💖 Support the Project

If JPersist is saving your engineering team debugging hours, consider backing us!

👉 **[Become a GitHub Sponsor](https://github.com/sponsors/jpersist)**

## 📄 License

Distributed under the Apache License 2.0. See the [LICENSE](LICENSE.txt) file for more information.
