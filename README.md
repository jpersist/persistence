# jPersist: Build powerful JPA modules with ease

![GitHub Release](https://img.shields.io/github/v/release/jpersist/persistence)
![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.jpersist.jpa)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jpersist_persistence&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jpersist_persistence)
![GitHub License](https://img.shields.io/github/license/jpersist/persistence)

A suite of modern, lightweight, high-performance, and **Gradle Configuration Cache-compliant** plugins to simplify development with the most popular **Jakarta Persistence API (JPA)** frameworks: **EclipseLink** and **Hibernate**.

The goal of this ecosystem is to decouple boilerplate configuration, automate static metamodel generation, and support compile-time bytecode enhancement seamlessly without violating incremental build integrity.

> 📖 **API Documentation:** Browse the full Groovydoc at [jpersist.github.io/persistence](https://jpersist.github.io/persistence/)

---

## What's New in 1.4.0

Version **1.4.0** introduces several major improvements:

- **Out-of-the-box JPA schema support from 2.0 to 3.2** — Descriptor processing now covers all JPA specification versions (2.0, 2.1, 2.2, 3.0, 3.1, 3.2) via a new strategy/delegate architecture. The appropriate XML namespace and schema location are resolved automatically based on the configured version.
- **Customizable `jar-file` location** — The `jarFile` dependency declaration now supports a custom location attribute (`io.github.jpersist.jpa.location`) that controls the path prefix written into `<jar-file>` elements. This is useful when JAR artifacts are deployed to a non-default directory within the application archive.
- **Explicit task dependency for EclipseLink weaving** — The `eclipseWeaveClasses` task now declares the `processPersistenceDescriptor` output file as an explicit input, establishing an immutable task execution graph dependency that guarantees descriptor generation completes before weaving begins.
- **Removed implicit `implementation` configuration** — JPA engine dependencies are no longer added to the `implementation` configuration implicitly, giving projects full control over their dependency scopes.
- **Removed Eclipse Buildship integration** — The automatic Eclipse IDE classpath synchronization hooks have been removed, simplifying the plugin internals.

---

## Repository Structure

The suite is organized into four primary submodules, each containing fully documented, type-safe Gradle plugins:
* **`persistence-gradle-plugin`** — Platform alignment plugin for centralized JPA dependency version management.
* **`jakarta-persistence-gradle-plugin`** — Generates or merges `persistence.xml` descriptors using a declarative DSL.
* **`eclipse-persistence-gradle-plugin`** — Houses all EclipseLink-related tooling and processing enhancements.
* **`hibernate-persistence-gradle-plugin`** — Houses all Hibernate-related static generation and enhancement utilities.

---

## Platform Plugin

The `persistence-gradle-plugin` module provides a foundational plugin for centralized dependency version management:

> ⚠️ **Deprecation Notice:** Starting from version **1.2.0**, the `persistence-gradle-plugin` and its `persistence` configuration will be deprecated in favor of the `jakarta-persistence-gradle-plugin`, which provides the same functionality through the `jpa` configuration. Users are encouraged to migrate to the `io.github.jpersist.jpa` plugin.

### `io.github.jpersist.persistence-platform`
Introduces a dedicated `persistence` configuration that allows you to declare a JPA implementation platform (BOM) dependency. Version constraints from the declared platform are automatically propagated into all standard Java configurations (`implementation`, `compileOnly`, `annotationProcessor`, `runtimeOnly`, and `testImplementation`), so individual JPA dependencies no longer need explicit version strings.

<details open>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/groovy/groovy-original.svg" width="16" height="16" valign="middle" alt="Groovy">
    <b>Groovy DSL</b>
  </summary>
  <br>

```groovy
plugins {
    id 'io.github.jpersist.hibernate-persistence'  // or 'io.github.jpersist.eclipse-persistence'
}

dependencies {
    persistence platform('org.hibernate.orm:hibernate-platform:6.6.5.Final')
}
```
</details>

<details>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original.svg" width="16" height="16" valign="middle" alt="Kotlin">
    <b>Kotlin DSL</b>
  </summary>
  <br>

```kotlin
plugins {
    id("io.github.jpersist.hibernate-persistence")  // or "io.github.jpersist.eclipse-persistence"
}

dependencies {
    persistence(platform("org.hibernate.orm:hibernate-platform:6.6.5.Final"))
}
```
</details>

> **Note:** The aggregate plugins (`hibernate-persistence` and `eclipse-persistence`) automatically apply the platform plugin, so you only need to declare the `persistence` dependency.

---

## Jakarta Persistence Plugin

The `jakarta-persistence-gradle-plugin` module provides a plugin for generating and processing JPA `persistence.xml` descriptors:

### `io.github.jpersist.jpa`
Registers a `processPersistenceDescriptor` task **for each Java source set** that either generates a complete `persistence.xml` from scratch using the `persistence` DSL, or merges extension-defined overrides (provider, data source, properties, jar-file entries) into an existing user-provided template. The `persistence` extension is a `NamedDomainObjectContainer` keyed by source set name (e.g. `main`, `test`), and each entry is automatically created when the source set is registered. It also introduces a `jpa` configuration for declaring platform/BOM version constraints that are injected into standard Java configurations (`implementation`, `compileOnly`, `annotationProcessor`, `runtimeOnly`). When the `java-library` plugin is applied, the `api` and `compileOnlyApi` configurations are also extended from `jpa`.

The plugin also provides a `jarFile` configuration that allows you to declare project or external dependencies whose resolved artifact names are automatically injected as `<jar-file>` elements in the generated `persistence.xml`. The `jarFile` configuration extends `implementation` (and `api` when the `java-library` plugin is present). This is particularly useful in multi-module projects where a persistence unit needs to reference entity classes packaged in separate JAR modules.

Persistence units already declared in a user-provided `persistence.xml` template are automatically registered into the extension at configuration time, allowing build scripts to customise them (e.g. disable or override properties) without re-declaring them in the DSL.

<details open>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/groovy/groovy-original.svg" width="16" height="16" valign="middle" alt="Groovy">
    <b>Groovy DSL</b>
  </summary>
  <br>

```groovy
plugins {
    id 'io.github.jpersist.jpa'
}

dependencies {
    compileOnly 'jakarta.persistence:jakarta.persistence-api:3.1.0'
    jarFile project(':common-persistence-module')
}

persistence {
    main {
        persistenceUnits {
            'example-persistence-unit' {
                provider = 'org.hibernate.jpa.HibernatePersistenceProvider'

                properties {
                    property 'hibernate.show_sql', 'true'
                }
            }
        }
    }
}
```
</details>

<details>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original.svg" width="16" height="16" valign="middle" alt="Kotlin">
    <b>Kotlin DSL</b>
  </summary>
  <br>

```kotlin
plugins {
    id("io.github.jpersist.jpa")
}

dependencies {
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
    "jarFile"(project(":common-persistence-module"))
}

persistence {
    named("main") {
        persistenceUnits {
            create("example-persistence-unit") {
                provider.set("org.hibernate.jpa.HibernatePersistenceProvider")

                properties {
                    property("hibernate.show_sql", "true")
                }
            }
        }
    }
}
```
</details>

#### Skipping Persistence Unit Generation

Individual persistence units can be excluded from the generated `persistence.xml` by setting their `enabled` property to `false`. When all units are disabled, the `processPersistenceDescriptor` task is automatically skipped. This allows build scripts to selectively disable units without removing their configuration:

<details open>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/groovy/groovy-original.svg" width="16" height="16" valign="middle" alt="Groovy">
    <b>Groovy DSL</b>
  </summary>
  <br>

```groovy
persistence {
    main {
        persistenceUnits {
            'my-unit' {
                enabled = false
            }
        }
    }
}
```
</details>

<details>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original.svg" width="16" height="16" valign="middle" alt="Kotlin">
    <b>Kotlin DSL</b>
  </summary>
  <br>

```kotlin
persistence {
    named("main") {
        persistenceUnits {
            named("my-unit") {
                enabled.set(false)
            }
        }
    }
}
```
</details>

#### Formatting the Generated XML

The XML output of the generated `persistence.xml` can be customized via the `transformer` DSL block or the `outputProperty` method. Default settings include `indent=yes`, `omit-xml-declaration=no`, `encoding=UTF-8`, and an indent amount of `4` spaces:

<details open>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/groovy/groovy-original.svg" width="16" height="16" valign="middle" alt="Groovy">
    <b>Groovy DSL</b>
  </summary>
  <br>

```groovy
persistence {
    main {
        transformer {
            outputProperty 'indent', 'yes'
            outputProperty '{http://xml.apache.org/xslt}indent-amount', '2'
        }
    }
}
```
</details>

<details>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original.svg" width="16" height="16" valign="middle" alt="Kotlin">
    <b>Kotlin DSL</b>
  </summary>
  <br>

```kotlin
persistence {
    named("main") {
        transformer {
            outputProperty("indent", "yes")
            outputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }
    }
}
```
</details>

---

## EclipseLink Plugins

These plugins are managed inside the `eclipse-persistence-gradle-plugin` module. They natively support and auto-initialize separate configuration contexts per active project source set (`main`, `test`, `integrationTest`, etc.) via a dynamic `NamedDomainObjectContainer`:

### 1. `io.github.jpersist.eclipse-jpa-modelgen`
Generates your JPA static canonical metamodel using the EclipseLink JPA Modelgen Processor. It automatically hooks into your compilation pipeline to track changes and produce your `_` metadata classes smoothly.

### 2. `io.github.jpersist.eclipse-static-weave`
Executes EclipseLink static weaving at compile-time via an isolated forked worker process (`javaexec`). It reads raw compiled bytecode and optimizes it to support lazy loading hooks, fetch graph optimizations, and advanced dirty tracking without needing a dynamic `-javaagent` at runtime.

### 3. `io.github.jpersist.eclipse-persistence`
An aggregate utility plugin designed for maximum simplicity. Applying this single identifier acts as a macro shortcut that automatically enables both `eclipse-jpa-modelgen` and `eclipse-static-weave` inside your project.

#### EclipseLink DSL Customization
By default, the plugin isolates contexts per source set and maps conventions natively. If your target configuration file lives in a non-standard path, you can easily override it inside your `build.gradle`:

<details open>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/groovy/groovy-original.svg" width="16" height="16" valign="middle" alt="Groovy">
    <b>Groovy DSL</b>
  </summary>
  <br>

```groovy
eclipselink {
    main {
        jpaModelgen {
            persistenceXml = 'src/main/resources/custom/persistence.xml'
        }
    }
    test {
        jpaModelgen {
            persistenceXml = 'src/test/resources/custom/persistence.xml'
        }
    }
}
```
</details>

<details>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original.svg" width="16" height="16" valign="middle" alt="Kotlin">
    <b>Kotlin DSL</b>
  </summary>
  <br>

```kotlin
the<NamedDomainObjectContainer<persist.eclipse.gradle.extension.EclipselinkExtension>>().named("main") {
    jpaModelgen {
        persistenceXml = "src/main/resources/custom/persistence.xml"
    }
}
the<NamedDomainObjectContainer<persist.eclipse.gradle.extension.EclipselinkExtension>>().named("test") {
    jpaModelgen {
        persistenceXml = "src/test/resources/custom/persistence.xml"
    }
}
```
</details>

---

## Hibernate Plugins

These plugins are managed inside the `hibernate-persistence-gradle-plugin` module:

### 1. `io.github.jpersist.hibernate-jpamodelgen`
Generates the JPA static canonical metamodel utilizing the Hibernate JPA Annotation Processor, helping you construct type-safe Criteria API queries flawlessly.

### 2. `io.github.jpersist.hibernate-enhancement`
A high-performance task that programmatically triggers Hibernate's core bytecode `Enhancer` engine. It isolates input and staging tracks, allowing full compatibility with Gradle's Configuration Cache and incremental execution engines.

### 3. `io.github.jpersist.hibernate-persistence`
An aggregate utility plugin that configures a complete standard Hibernate baseline out of the box. It automatically applies `hibernate-jpamodelgen` alongside `hibernate-enhancement`.

#### Hibernate DSL Customization
Sane optimization defaults are configured automatically. You can cleanly adjust or override enhancement behaviors via the following block:

<details open>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/groovy/groovy-original.svg" width="16" height="16" valign="middle" alt="Groovy">
    <b>Groovy DSL</b>
  </summary>
  <br>

```groovy
hibernate {
    enhancement {
        enableLazyInitialization = true
        enableDirtyTracking = true
        enableAssociationManagement = true
        enableExtendedEnhancement = false
    }
}
```
</details>

<details>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original.svg" width="16" height="16" valign="middle" alt="Kotlin">
    <b>Kotlin DSL</b>
  </summary>
  <br>

```kotlin
hibernate {
    enhancement {
        enableLazyInitialization.set(true)
        enableDirtyTracking.set(true)
        enableAssociationManagement.set(true)
        enableExtendedEnhancement.set(false)
    }
}
```
</details>

---

## Dependency & Version Resolution

⚠️ **Important:** These plugins add underlying engine dependencies (such as `hibernate-core`, `eclipselink`, or `jakarta.persistence-api`) **without hardcoded version strings**. The consuming project **must** explicitly provide version information. If you omit versions, Gradle will fail with a resolution error.

### Option A: Using the `persistence` Configuration (Recommended)
The simplest and recommended approach — declare a platform BOM on the `persistence` configuration. Versions are automatically aligned across all configurations:

> 📌 **Note:** Starting from version **1.2.0**, the `persistence` configuration will become `jpa`, providing the same functionality through the `jakarta-persistence-gradle-plugin`.

<details open>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/groovy/groovy-original.svg" width="16" height="16" valign="middle" alt="Groovy">
    <b>Groovy DSL</b>
  </summary>
  <br>

```groovy
plugins {
    id 'io.github.jpersist.hibernate-persistence'
}

dependencies {
    // For Hibernate ecosystems
    persistence platform('org.hibernate.orm:hibernate-platform:6.6.5.Final')

    // For EclipseLink ecosystems
    // persistence platform('org.eclipse.persistence:org.eclipse.persistence.parent:4.0.9')
}
```
</details>

<details>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original.svg" width="16" height="16" valign="middle" alt="Kotlin">
    <b>Kotlin DSL</b>
  </summary>
  <br>

```kotlin
plugins {
    id("io.github.jpersist.hibernate-persistence")
}

dependencies {
    // For Hibernate ecosystems
    persistence(platform("org.hibernate.orm:hibernate-platform:6.6.5.Final"))

    // For EclipseLink ecosystems
    // persistence(platform("org.eclipse.persistence:org.eclipse.persistence.parent:4.0.9"))
}
```
</details>

### Option B: Using a Gradle Version Catalog
Define explicit versions in your project's `gradle/libs.versions.toml`:

```toml
[versions]
eclipselink = "4.0.9"
hibernate = "6.6.5.Final"

[libraries]
eclipselink-jpa = { group = "org.eclipse.persistence", name = "org.eclipse.persistence.jpa", version.ref = "eclipselink" }
hibernate-core = { group = "org.hibernate.orm", name = "hibernate-core", version.ref = "hibernate" }
```

Then apply them inside your submodule dependencies:

<details open>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/groovy/groovy-original.svg" width="16" height="16" valign="middle" alt="Groovy">
    <b>Groovy DSL</b>
  </summary>
  <br>

```groovy
dependencies {
    implementation libs.eclipselink.jpa // or libs.hibernate.core
}
```
</details>

<details>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original.svg" width="16" height="16" valign="middle" alt="Kotlin">
    <b>Kotlin DSL</b>
  </summary>
  <br>

```kotlin
dependencies {
    implementation(libs.eclipselink.jpa) // or libs.hibernate.core
}
```
</details>

### Option C: Using an Official Platform / BOM Directly
Enforce consistency using an upstream Bill of Materials platform on `implementation`:

<details open>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/groovy/groovy-original.svg" width="16" height="16" valign="middle" alt="Groovy">
    <b>Groovy DSL</b>
  </summary>
  <br>

```groovy
dependencies {
    implementation platform('org.hibernate.orm:hibernate-platform:6.6.5.Final')
}
```
</details>

<details>
  <summary>
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original.svg" width="16" height="16" valign="middle" alt="Kotlin">
    <b>Kotlin DSL</b>
  </summary>
  <br>

```kotlin
dependencies {
    implementation(platform("org.hibernate.orm:hibernate-platform:6.6.5.Final"))
}
```
</details>

---

## Advanced Ecosystem Performance Details

### 1. High-Speed Incremental Builds & `NO-SOURCE` Mapping
All transformation tasks employ native Gradle `@SkipWhenEmpty` mechanics. If a targeted source set contains no source files (e.g., a module with no active test classes), the plugin tasks immediately exit with a true **`NO-SOURCE`** or **`SKIPPED`** outcome, preventing unnecessary execution overhead.

### 2. Comprehensive IDE Quick-Doc Support
Every public API boundary—including all Plugin classes, independent Task structures, and nested DSL Extension properties—carries explicit, strongly-typed Groovydoc declarations. This ensures immediate type safety, strict compile-time validation, and detailed inline helper tooltips when developing inside IntelliJ IDEA or Eclipse.

---

## Usage Examples

Complete, ready-to-use example projects are available in the [`examples/`](examples/) directory:

| Example | Description |
|---------|-------------|
| [`eclipselink-example/`](examples/eclipselink-example/) | EclipseLink with metamodel generation, static weaving, and platform BOM |
| [`hibernate-example/`](examples/hibernate-example/) | Hibernate ORM with metamodel generation, bytecode enhancement, and platform BOM |
| [`jakarta-persistence-example/`](examples/jakarta-persistence-example/) | Jakarta Persistence descriptor generation and merging with the `io.github.jpersist.jpa` plugin, including skip, formatting, auto-registration, and `java-library` integration features |

Each example includes both **Groovy DSL** (`build.gradle`) and **Kotlin DSL** (`build.gradle.kts`) build files.

---

## Requirements

- **Gradle:** 7.4+ or 8.x+
- **Java:** JDK 17 or higher (Required by modern Jakarta Persistence specifications)

## License

Distributed under the Apache License 2.0. See the [LICENSE.txt](LICENSE.txt) file for more information.
