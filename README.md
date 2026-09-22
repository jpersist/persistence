# jPersist: Build powerful JPA modules with ease

A suite of modern, lightweight, and **Gradle Configuration Cache-compliant** plugins to simplify development with the most popular **Jakarta Persistence API (JPA)** frameworks: **EclipseLink** and **Hibernate**.

The goal of this ecosystem is to decouple boilerplate configuration, automate static metamodel generation, and support compile-time bytecode weaving seamlessly.

---

## Repository Structure

The suite is organized into two primary submodules:
* **`eclipse-persistence-gradle-plugin`** — Houses all EclipseLink-related tooling and processing enhancements.
* **`hibernate-persistence-gradle-plugin`** — Houses all Hibernate-related static generation and enhancement utilities.

---

## EclipseLink Persistence Plugins

These plugins are managed inside the `eclipse-persistence-gradle-plugin` module:

### 1. `io.github.jpersist.eclipse-jpa-modelgen`
Generates your JPA static canonical metamodel using the EclipseLink JPA Modelgen Processor. It automatically hooks into your compilation pipeline to track changes and produce your `_` metadata classes smoothly.

### 2. `io.github.jpersist.eclipse-static-weave`
Executes EclipseLink static weaving at compile-time directly over your target output directory. This weaves your entity bytecode in-place to support strict lazy loading hooks, fetch optimizations, and advanced dirty tracking without needing a dynamic javaagent at runtime.

### 3. `io.github.jpersist.eclipse-persistence`
An aggregate utility plugin designed for maximum simplicity. Applying this single plugin automatically enables both `eclipse-jpa-modelgen` and `eclipse-static-weave` inside your target submodule.

---

## Hibernate Persistence Plugins

These plugins are managed inside the `hibernate-persistence-gradle-plugin` module:

### 1. `io.github.jpersist.hibernate-jpamodelgen`
Generates the JPA static canonical metamodel utilizing the Hibernate JPA Annotation Processor, helping you construct type-safe Criteria queries flawlessly.

### 2. `io.github.jpersist.hibernate-persistence`
An aggregate utility plugin that configures a complete corporate standard Hibernate baseline out of the box. It applies `hibernate-jpamodelgen` alongside the official `org.hibernate.orm` toolbelt, applying sane default configurations for bytecode enhancement (such as lazy initialization and associations management) while allowing you to effortlessly override them.

---

## IMPORTANT: Dependency & Version Resolution Requirements

⚠️ **Crucial Requirement:** To prevent library conflicts within your application, these plugins add underlying engine dependencies (such as `hibernate-core`, `eclipselink`, or `jakarta.persistence-api`) **WITHOUT hardcoded version strings**.

The consuming project **MUST** explicitly manage and resolve version numbers using one of the standard Gradle dependency management strategies outlined below. If you omit versions in your project, Gradle will fail with a resolution error.

### Option A: Using a Gradle Version Catalog (Recommended)
Define the explicit versions in your `gradle/libs.versions.toml`:

```toml
[versions]
eclipselink = "4.0.9"
hibernate = "6.6.0.Final"

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
Enforce consistency across your entire project configurations using a Bill of Materials platform:

```groovy
dependencies {
    // For Hibernate ecosystems
    implementation platform('org.hibernate.orm:hibernate-platform:6.6.0.Final')

    // For EclipseLink ecosystems
    implementation platform('org.eclipse.persistence:org.eclipse.persistence.parent:4.0.9')
}
```

---

## Configuration & Usage

### 1. EclipseLink Modelgen Customization
By default, the EclipseLink canonical model processor dynamically searches for your standard `persistence.xml` context file path. If your configuration lives in a non-standard location, customize it via the extension:

```groovy
eclipselink {
    jpaModelgen {
        persistenceXml = 'src/custom/location/persistence.xml'
    }
}
```

---

## Requirements

- **Gradle:** 7.4+ or 8.x+
- **Java:** JDK 17 or higher (Required by modern Jakarta Persistence specifications)

## License

Distributed under the Apache License 2.0. See the [LICENSE.txt](LICENSE.txt) file for more information.
