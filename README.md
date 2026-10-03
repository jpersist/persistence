# 📦 JPersist: Zero-Boilerplate JPA Build Automation for Gradle

![GitHub Release](https://img.shields.io/github/v/release/jpersist/persistence)
![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.jpersist.jpa)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence)
![GitHub License](https://img.shields.io/github/license/jpersist/persistence)

**JPersist** is a suite of modern, high-performance, and **Gradle Configuration Cache-compliant** plugins that completely automates the tedious boilerplate configuration of enterprise **Jakarta Persistence API (JPA)** environments. It provides zero-configuration support for **Hibernate** and **EclipseLink** multi-module builds.

---

## 🚀 Why JPersist?

Standard Gradle loops require verbose XML overrides, break incremental compilation states during bytecode manipulation, and crash with complex classpath tasks decoration proxy loops. **JPersist fixes this permanently.**

| Standard Gradle Setup | The JPersist Masterclass Advantage |
| :--- | :--- |
| ❌ Manual `<class>` tagging required | **✅ ASM-Powered Bytecode Scanning** auto-detects entities |
| ❌ Hardcoded schema structures | **✅ Strategy-Driven Polymorphism** supports JPA 2.0 to 3.2 dynamically |
| ❌ Rigid monolithic output paths | **✅ Declarative Attributes** route individual JAR locations via the DSL |
| ❌ Broken workspace sync mappings | **✅ Automated IDE Indexing** hooks into Buildship and IntelliJ out-of-the-box |

---

## 💖 Support the Project

Running complex continuous integration checks, multi-spec regression testing matrices, and maintaining full compatibility with rapid Gradle releases requires continuous investment.

If JPersist is saving your engineering team debugging overhead hours, consider backing us!

👉 **[Become a GitHub Sponsor to support JPersist's independence](https://github.com/jpersist/persistence)**

---

## ⚡ Quick Start

Apply the aggregate ecosystem plugin matching your infrastructure stack. Version suggestions from your corporate Version Catalogs or platform BOMs are dynamically propagated into all standard Java compilation scopes.

### For Hibernate 6.x+ Projects
<details open>
  <summary><b>Groovy DSL</b></summary>
  <br>

```groovy
plugins {
    id 'io.github.jpersist.hibernate-persistence' // Enables descriptor generation, modelgen, & enhancement
}

dependencies {
    // Centralize versioning via a BOM platform configuration
    jpa platform('org.hibernate.orm:hibernate-platform:6.6.56.Final')
}
```
</details>

<details>
  <summary><b>Kotlin DSL</b></summary>
  <br>

```kotlin
plugins {
    id("io.github.jpersist.hibernate-persistence")
}

dependencies {
    "jpa"(platform("org.hibernate.orm:hibernate-platform:6.6.56.Final"))
}
```
</details>

### For EclipseLink Projects
<details open>
  <summary><b>Groovy DSL</b></summary>
  <br>

```groovy
plugins {
    id 'io.github.jpersist.eclipse-persistence' // Enables descriptor generation, modelgen, & static weaving
}

dependencies {
    jpa platform('org.eclipse.persistence:org.eclipse.persistence.parent:4.0.9')
}
```
</details>

---

## 💎 Core Production-Ready Features

### 🛠️ Polymorphic Descriptor Merging & Generation (`io.github.jpersist.jpa`)
Registers a `processPersistenceDescriptor` task **for each Java source set**. It parses existing user templates, resolves conflicts, and writes a pristine, **4-space indented, pretty-printed** output into your build resources.
* **Implicit Version Sync:** Automatically extracts version attributes from physical source XMLs at configuration time to handle schema modifications dynamically.
* **Declarative Dependency Routing:** Pass custom namespaced attributes to control exactly what path prefix is written inside `<jar-file>` element rows for custom archive environments:

```groovy
dependencies {
    jarFile(project(':common-entities')) {
        attributes {
            attribute(persist.jakarta.gradle.plugin.JakartaPersistencePlugin.JAR_LOCATION_ATTRIBUTE, 'lib/')
        }
    }
}
```

### 🧵 Isolated Bytecode Static Weaving & Enhancement
Triggers raw bytecode modification tasks post-compilation, modifying entities safely in-place within the compilation pipeline boundaries.
* **Proxy-Free Safety:** Execution loops are fully encapsulated within an isolated static helper layer, making it completely immune to Gradle proxy object `MissingMethodException` bugs.
* **No Runtime Agents:** Natively supports performance optimizations like lazy loading hooks and advanced inline dirty tracking without requiring an active runtime `-javaagent` argument.

---

## 📂 Repository Blueprint

* **`jakarta-persistence-gradle-plugin`** — Generates or merges `persistence.xml` descriptors using a declarative DSL.
* **`eclipse-persistence-gradle-plugin`** — Houses all EclipseLink-related tooling and processing enhancements.
* **`hibernate-persistence-gradle-plugin`** — Houses all Hibernate-related static generation and enhancement utilities.

> 📖 **API Reference:** Browse the full Groovydoc at [jpersist.github.io/persistence](https://jpersist.github.io/persistence/)
> 🚀 **Runnable Specs:** Complete code templates are available in the [`examples/`](examples/) directory.

---

## 📌 Requirements

- **Gradle:** 7.4+ or 8.x+ / 9.x+
- **Java:** JDK 17 or higher (Required by modern Jakarta specifications)

## 📄 License

Distributed under the Apache License 2.0. See the [LICENSE](LICENSE.txt) file for more information.
