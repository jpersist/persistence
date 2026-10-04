plugins {
    `java-library`
    id("io.github.jpersist.hibernate-persistence") version "1.5.0"
}

dependencies {
    jpa(platform("org.hibernate.orm:hibernate-platform:6.6.56.Final"))

    implementation("org.hibernate.orm:hibernate-core")
}

repositories {
    mavenCentral()
}
