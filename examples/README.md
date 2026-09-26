# JPersist Plugin Examples

This directory contains example projects that showcase the JPersist Gradle plugins.

## Hibernate Example

The [`hibernate-example`](hibernate-example) project demonstrates how to use the `io.github.jpersist.hibernate-persistence` plugin. It applies Hibernate bytecode enhancement and JPA metamodel generation to a simple `Person` entity.

### Usage

```bash
cd examples/hibernate-example
gradle build
```

## EclipseLink Example

The [`eclipse-example`](eclipse-example) project demonstrates how to use the `io.github.jpersist.eclipse-persistence` plugin. It applies EclipseLink static weaving and JPA metamodel generation to a simple `Person` entity.

### Usage

```bash
cd examples/eclipse-example
gradle build
```
