plugins {
    `java-library`
    id("io.github.jpersist.openjpa-persistence")
    id("com.netflix.nebula.dependency-recommender") version "14.2.1"
}

dependencies {
    jpa("org.apache.openjpa:openjpa")

    compileOnly("jakarta.persistence:jakarta.persistence-api")
}

dependencyRecommendations {
    map(mapOf("recommendations" to mapOf(
        "jakarta.persistence:jakarta.persistence-api" to "3.1.0",
        "org.apache.openjpa:openjpa"                  to "4.0.0"
    )))
}

openjpa {
    main {
        metamodel {
            logLevel.set(MetaModelExtension.LogLevel.TRACE)
        }
        enhancement {
            addDefaultConstructor.set(true)
            enforcePropertyRestrictions.set(false)
        }
    }
}

repositories {
    mavenCentral()
}
