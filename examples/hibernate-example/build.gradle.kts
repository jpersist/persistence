plugins {
    `java-library`
    id("io.github.jpersist.hibernate-persistence") version "1.1.0"
}

dependencies {
    persistence(platform("org.hibernate.orm:hibernate-platform:6.6.56.Final"))

    implementation("org.hibernate.orm:hibernate-core")
}

repositories {
    mavenCentral()
}
