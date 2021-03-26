# Violet [![](https://www.jitpack.io/v/NewNanCity/Violet.svg)](https://www.jitpack.io/#NewNanCity/Violet) [![](https://img.shields.io/badge/Join-NewNanCity-yellow)](https://www.newnan.city)

Useful toolkits java library for Bukkit Server Plugin.

- [x] ConfigManager
- [x] MessageManager (i18n Supported)
- [x] LanguageManager
- [x] CommandManager (Deprecated, and recommend to use [aikar's commands](https://github.com/aikar/commands))

## How to add Violet to your project

## Maven

Add the JitPack repository to your build file:

```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://www.jitpack.io</url>
        </repository>
    </repositories>
```

Add the dependency:

```xml
    <dependency>
        <groupId>com.github.NewNanCity</groupId>
        <artifactId>Violet</artifactId>
        <version>VERSION</version>
    </dependency>
```

Gradle

Add it in your root build.gradle at the end of repositories:

```
    allprojects {
        repositories {
            ...
            maven { url 'https://www.jitpack.io' }
        }
    }
```

Add the dependency:

```
    dependencies {
        implementation 'com.github.NewNanCity:Violet:1.0.4'
    }
```