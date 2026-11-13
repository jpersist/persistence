# 📦 JPersist: Zero-Boilerplate JPA Build Automation for Gradle

[![Java Gradle CI](https://github.com/jpersist/persistence/actions/workflows/gradle-ci.yml/badge.svg)](https://github.com/jpersist/persistence/actions/workflows/gradle-ci.yml)
![GitHub Release](https://img.shields.io/github/v/release/jpersist/persistence)
![GitHub License](https://img.shields.io/github/license/jpersist/persistence)

### 📊 Component Status Matrices

| Component | Release Version | Quality Gate | Code Coverage |
| :--- | :--- | :--- | :--- |
| **🔌 Gradle Plugins** | ![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.jpersist.jpa) | [![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_plugins&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_plugins) | [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_plugins&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_plugins) |
| **📦 Core Libraries** | ![Maven Central](https://img.shields.io/maven-central/v/io.github.jpersist.tools/persistence-tools-api) | [![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries) | [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence_libraries&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence_libraries) |

---

## 🧩 What is JPersist?

**JPersist** is the ultimate developer productivity suite for enterprise **Jakarta Persistence API (JPA)** on Gradle. Built from the ground up for modern enterprise engineering teams, JPersist delivers a frictionless, **zero-boilerplate build automation platform** that eliminates manual configuration, accelerates developer onboarding, and guarantees robust, high-performance builds.

From automated descriptor generation and bytecode enhancement to instant schema validation, query linting, and GraalVM native image readiness, JPersist allows developers to focus on delivering business value rather than maintaining fragile build scripts.

---

## 🏆 Why Choose JPersist?

Modern enterprise applications require robust persistence architectures, but standard build configurations are notoriously fragile, slow to set up, and prone to silent runtime failures. **JPersist eliminates this friction entirely.**

| Traditional Build Setup               | The JPersist Advantage                                                             | Business Impact                                      |
|:--------------------------------------|:-----------------------------------------------------------------------------------|:-----------------------------------------------------|
| ❌ Manual `<class>` entity tagging     | **✅ Instant entity discovery** via automated bytecode scanning                   | ⚡ **Faster Onboarding** — Zero boilerplate to write |
| ❌ Runtime query & mapping errors     | **✅ Compile-time query linting & schema validation**                              | 🛡️ **Zero Runtime Surprises** — Catch bugs in CI     |
| ❌ Complex performance tuning setup   | **✅ Automatic bytecode enhancement & weaving** (Hibernate, EclipseLink, OpenJPA)  | 🚀 **Peak Performance** — Optimized dirty tracking   |
| ❌ Fragile Native & Modular setups    | **✅ Out-of-the-box GraalVM metadata & JPMS modular support**                      | ☁️ **Cloud Native Ready** — Fast boot & low memory   |
| ❌ Broken IDE workspace sync          | **✅ Seamless IDE integration** for IntelliJ IDEA and Eclipse Buildship            | 💻 **Frictionless DX** — Developers stay in the flow |

### ⚡ Key Capabilities

- **Zero-Config Descriptor Generation** — Automatic entity discovery and `persistence.xml` generation across all source sets.
- **Automated Performance Enhancement** — Build-time bytecode enhancement for Hibernate, EclipseLink static weaving, and OpenJPA PCEnhancer.
- **Compile-Time Query Safety** — Static AST validation of `@NamedQuery` and stored procedure definitions to catch syntax errors during build.
- **Enterprise Schema Validation** — Automated database schema verification against live databases or multi-tenant database profiles directly in CI/CD.
- **Cloud-Native & Modular Ready** — Native AOT reflection configuration generation for GraalVM and zero-configuration Java Platform Module System (JPMS) support.
- **Lightning-Fast Builds** — Fully compatible with Gradle Configuration Cache and incremental builds for maximum throughput.

---

## 🚀 Getting Started

Apply the plugin matching your JPA provider. The `jpa` dependency configuration centralizes version management via a BOM platform across all standard Java compilation scopes.

### <img src="https://cdn.simpleicons.org/hibernate" alt="Hibernate" height="20"> Hibernate Setup

<details open>
  <summary><b>Groovy DSL</b></summary>
  <br>

```groovy
plugins {
    id 'io.github.jpersist.hibernate-persistence' version '1.9.0'
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
    id("io.github.jpersist.hibernate-persistence") version "1.9.0"
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
    id 'io.github.jpersist.eclipse-persistence' version '1.9.0'
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
    id("io.github.jpersist.eclipse-persistence") version "1.9.0"
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
    id 'io.github.jpersist.openjpa-persistence' version '1.9.0'
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
    id("io.github.jpersist.openjpa-persistence") version "1.9.0"
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
| `libraries/hibernate-orm-persistence-tools`          | Jakarta Persistence (3.x) linter implementation (`io.github.jpersist.tools:persistence-tools-jakarta`).                   |
| `libraries/hibernate-persistence-tools`           | Legacy JPA (2.x) linter implementation (`io.github.jpersist.tools:persistence-tools-legacy`).                             |
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
