# Violet [![](https://www.jitpack.io/v/NewNanCity/Violet.svg)](https://www.jitpack.io/#NewNanCity/Violet) [![](https://img.shields.io/badge/Join-NewNanCity-yellow)](https://www.newnan.city)

![Gk0Wk's GitHub stats](https://github-readme-stats.vercel.app/api?username=Gk0Wk&theme=dracula&show_icons=true&count_private=true)

Useful toolkits java library for Bukkit Server Plugin.

- [x] ConfigManager
- [x] MessageManager (i18n Supported)
- [x] LanguageManager
- [x] CommandManager (Deprecated, and recommend to use [aikar's commands](https://github.com/aikar/commands))
- [x] DatabaseManager (Now just MySQL)
- [x] NetworkManager (Based on okHttp4)

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

## Gradle

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
        implementation 'com.github.NewNanCity:Violet:VERSION'
    }
```
