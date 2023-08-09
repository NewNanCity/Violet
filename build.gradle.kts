import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

val targetJavaVersion = 8

version = "2.1.0"
group = "city.newnan.violet"
description = "Useful toolkits java library for Bukkit Server Plugin."

plugins {
    id("idea")
    id("maven-publish")
    kotlin("jvm") version "1.6.21"
}

repositories {
    mavenLocal()
    maven("https://repo.lucko.me/")
    maven("https://libraries.minecraft.net/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    mavenCentral()
}

dependencies {
    // Minecraft
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    // Utils
    compileOnly("me.lucko:helper:5.6.13")
    // Database
    compileOnly("mysql:mysql-connector-java:8.0.33")
    compileOnly("com.zaxxer:HikariCP:4.0.3")
    compileOnly("org.ktorm:ktorm-core:3.6.0")
    // Network
    compileOnly("com.squareup.okhttp3:okhttp:4.11.0")
    // ConfigureFile
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.15.2")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:2.15.2")
    compileOnly("com.jasonclawson:jackson-dataformat-hocon:1.1.0")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    // Test
    testImplementation(kotlin("test"))
    // Minecraft
    testImplementation("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    // Utils
    testImplementation("me.lucko:helper:5.6.13")
    // Database
    testImplementation("mysql:mysql-connector-java:8.0.33")
    testImplementation("com.zaxxer:HikariCP:4.0.3")
    testImplementation("org.ktorm:ktorm-core:3.6.0")
    // Network
    testImplementation("com.squareup.okhttp3:okhttp:4.11.0")
    // ConfigureFile
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.15.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:2.15.2")
    testImplementation("com.jasonclawson:jackson-dataformat-hocon:1.1.0")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
}

tasks.test { useJUnitPlatform() }

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.withType<JavaCompile>().configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.toVersion(targetJavaVersion).toString()
}


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                // Applies the component for the release build variant.
                from(components["kotlin"])
                // You can then customize attributes of the publication as shown below.
                groupId = project.group as String
                artifactId = project.name
                version = project.version as String
                pom {
                    name.set(project.name)
                    description.set(project.description!!)
                    url.set("https://github.com/NewNanCity/Violet")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://mit-license.org/")
                        }
                    }
                    developers {
                        developer {
                            id.set("Gk0Wk")
                            name.set("Gk0Wk")
                            email.set("nmg_wk@yeah.net")
                            url.set("https://github.com/Gk0Wk")
                            organization.set("NewNanCity")
                            organizationUrl.set("https://github.com/NewNanCity")
                        }
                    }
                }
            }
        }
    }
}

