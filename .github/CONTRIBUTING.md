# Contributing to JPersist 🚀

First off, thank you for taking the time to contribute! It is developers like you who make JPersist an incredible tool for the entire JVM ecosystem.

## 🛠️ Local Environment Setup

Since JPersist relies on a modular Composite Build architecture, setting up your workspace is simple:

1. Fork and clone the repository.
2. Ensure you have **JDK 17** installed natively.
3. Run a baseline verification build to ensure your environment is clean:
   ```bash
   ./gradlew clean build
   ```

## 📐 Coding Standards & Guidelines

To keep the repository history immaculate and maintain our enterprise-grade codebase:
* **Style:** Follow standard Groovy/Java conventions. Ensure all dynamic strings inside deferred task closures explicitly invoke `.toString()` to prevent lifecycle scope mutation bugs.
* **Testing:** Every new feature or bugfix **must** be accompanied by a corresponding Spock functional test case utilizing Gradle TestKit inside `:plugins:persistence-gradle-plugin-functional-test`.
* **Commits:** We strictly enforce the **Conventional Commits** specification (e.g., `feat(linter): ...`, `fix(weave): ...`).

## 📥 Pull Request (PR) Process

1. Create a descriptive feature branch (`feature/your-feature-name`).
2. Ensure `./gradlew check` passes completely green on your local machine.
3. Open a Pull Request targeting our `main` branch.
4. Describe your changes clearly and link any active issues. A maintainer will review your code within 48 business hours!
