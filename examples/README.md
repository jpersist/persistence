# JPersist Plugin Examples

This directory contains example projects that showcase the JPersist Gradle plugins.

## EclipseLink Example

The [`eclipse-example`](eclipse-example) project demonstrates how to use the `io.github.jpersist.eclipse-persistence` plugin. It applies EclipseLink static weaving and JPA metamodel generation to a simple `Person` entity.

### Usage

```bash
cd examples/eclipse-example
gradle build
```

## Hibernate Example

The [`hibernate-example`](hibernate-example) project demonstrates how to use the `io.github.jpersist.hibernate-persistence` plugin. It applies Hibernate bytecode enhancement and JPA metamodel generation to a simple `Person` entity.

### Usage

```bash
cd examples/hibernate-example
gradle build
```

## Jakarta Persistence Example

The [`jakarta-persistence-example`](jakarta-persistence-example) project demonstrates how to use the `io.github.jpersist.jpa` plugin. It processes a `persistence.xml` descriptor by merging extension-defined overrides into a user-provided template, or generates one from scratch using the `persistence` DSL.

### Usage

```bash
cd examples/jakarta-persistence-example
gradle build
```
