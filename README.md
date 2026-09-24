# jPersist: Build powerful JPA modules with ease

A suite of modern, lightweight, high-performance, and **Gradle Configuration Cache-compliant** plugins to simplify development with the most popular **Jakarta Persistence API (JPA)** frameworks: **EclipseLink** and **Hibernate**.

The goal of this ecosystem is to decouple boilerplate configuration, automate static metamodel generation, and support compile-time bytecode enhancement seamlessly without violating incremental build integrity.

---

## Repository Structure

The suite is organized into two primary submodules, each containing fully documented, type-safe Gradle plugins:
* **`eclipse-persistence-gradle-plugin`** — Houses all EclipseLink-related tooling and processing enhancements.
* **`hibernate-persistence-gradle-plugin`** — Houses all Hibernate-related static generation and enhancement utilities.

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

---

## IMPORTANT: Dependency & Version Resolution Requirements

⚠️ **Crucial Requirement:** To prevent library and runtime conflicts within your application, these plugins add underlying engine dependencies (such as `hibernate-core`, `eclipselink`, or `jakarta.persistence-api`) **WITHOUT hardcoded version strings**.

The consuming project **MUST** explicitly manage and resolve version numbers using one of the standard Gradle dependency management strategies outlined below. If you omit versions in your project, Gradle will fail with a resolution error.

### Option A: Using a Gradle Version Catalog (Recommended)
Define explicit versions in your project's `gradle/libs.versions.toml`:

```toml
[versions]
eclipselink = "4.0.9"
hibernate = "6.6.56.Final"

[libraries]
eclipselink-jpa = { group = "org.eclipse.persistence", name = "org.eclipse.persistence.jpa", version.ref = "eclipselink" }
hibernate-core = { group = "org.hibernate.orm", name = "hibernate-core", version.ref = "hibernate" }
```

Then apply them inside your submodule dependencies:
```groovy
dependencies {
    implementation libs.eclipselink.jpa // or libs.hibernate.core
}
```

### Option B: Using an Official Platform / BOM
Enforce consistency across your entire project configurations using an upstream Bill of Materials platform:

```groovy
dependencies {
    // For Hibernate ecosystems
    implementation platform('org.hibernate.orm:hibernate-platform:6.6.56.Final')

    // For EclipseLink ecosystems
    implementation platform('org.eclipse.persistence:org.eclipse.persistence.parent:4.0.9')
}
```

---

## Advanced Ecosystem Performance Details

### 1. High-Speed Incremental Builds & `NO-SOURCE` Mapping
All transformation tasks employ native Gradle `@SkipWhenEmpty` mechanics. If a targeted source set contains no source files (e.g., a module with no active test classes), the plugin tasks immediately exit with a true **`NO-SOURCE`** or **`SKIPPED`** outcome, preventing unnecessary execution overhead.

### 2. Eclipse IDE Integration
When applying any of the scoped plugins, the system hooks into Eclipse Buildship's synchronization lifecycle. When you perform a **Gradle -> Refresh Project** action:
- It checks if files were generated by the processor.
- It dynamically adds the generated annotation processor folders into Eclipse's source classpath with modern `test="true"` scoping parameters where applicable.
- It safely clears out the classpath entries if the output directory becomes empty to keep your workspace pristine.

### 3. Comprehensive IDE Quick-Doc Support
Every public API boundary—including all Plugin classes, independent Task structures, and nested DSL Extension properties—carries explicit, strongly-typed Groovydoc declarations. This ensures immediate type safety, strict compile-time validation, and detailed inline helper tooltips when developing inside IntelliJ IDEA or Eclipse.

---

## Requirements

- **Gradle:** 7.4+ or 8.x+
- **Java:** JDK 17 or higher (Required by modern Jakarta Persistence specifications)

## License

Distributed under the Apache License 2.0. See the [LICENSE.txt](LICENSE.txt) file for more information.
