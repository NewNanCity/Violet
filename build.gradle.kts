import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val groupIdVal = "city.newnan"
val versionVal = "2.0.8"

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

group = groupIdVal
version = versionVal

plugins {
    id("idea")
    id("maven-publish")
    kotlin("jvm") version "1.8.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenLocal()
    maven("https://repo.lucko.me/")
    maven("https://libraries.minecraft.net/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    // Minecraft
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    // Utils
    api("me.lucko:helper:5.6.13")
    // Database
    api("mysql:mysql-connector-java:8.0.33")
    api("com.zaxxer:HikariCP:4.0.3")
    api("org.ktorm:ktorm-core:3.6.0")
    // Network
    api("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
    // JSON
    api("com.lectra:koson:1.2.5")
    api("com.google.code.gson:gson:2.10.1")
    // JDK
    api("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")
    // ConfigureFile
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    // api("com.fasterxml.jackson.core:jackson-core:2.15.2")
    api("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.15.2")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:2.15.2")
    api("com.jasonclawson:jackson-dataformat-hocon:1.1.0")
    // Test
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

java.targetCompatibility = JavaVersion.VERSION_11
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                // Applies the component for the release build variant.
                from(components["kotlin"])
                // You can then customize attributes of the publication as shown below.
                groupId = groupIdVal
                artifactId = "Violet"
                version = versionVal
                pom {
                    name.set("Violet")
                    description.set("Useful toolkits java library for Bukkit Server Plugin.")
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
