plugins {
    `java-library`
    id("io.github.jpersist.jpa") version "1.3.0"
}

dependencies {
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
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

repositories {
    mavenCentral()
}
