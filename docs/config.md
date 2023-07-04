# ConfigManager

> ConfigManager2 is based on jackson.

Support:

* JSON, YAML, TOML, HOCON, Properties, XML, CSV.
* Tree-mode & Class-mapping-mode.
* Bukkit's ConfigurationSerializable like ItemStack, ItemMeta, Location, etc..

Setup:

```kotlin
// In plugin onLoad
val configManager = ConfigManager2(this)
```

Read config:

```kotlin
// Check if config exists
configManager.touch("config.yml")
// Get config
val config = configManager["config.yml"]
// Visit config
config.root {
    // it == ObjectNode of jackson
    val enableMultiHome = it["multi-home", "enable"].asBoolean()
    val homeLimit = it["multi-home", "limit"].asInt(1) // can set default value
    val inventory = it["inventory"].asBase64Inventory()
    val location = it["location"].asConfigurationSerializable<Location>()
}
```

Write:

```kotlin
// Write and save
configManager["store.json"].save {
    it.set("last-update", System.currentTimeMillis())
    it.set("last-player", Bukkit.getOfflinePlayers()[0])
    it.set("example-item", ItemStack(Material.APPLE, 64))
    it.setBase64("example-inventory", Bukkit.getOfflinePlayers()[0].player!!.inventory)
    it.remove("foo")
}
```

Multi-type support:

```kotlin
// Convert type
configManager["1.json"].saveAs("1.xml")
```

## About ObjectNode

Further reading:

- [Jackson Document](https://github.com/FasterXML/jackson-docs)
- [Databind annotations for class](https://stackabuse.com/definitive-guide-to-jackson-objectmapper-serialize-and-deserialize-java-objects/)

locate a noe:

```kotlin
node["123", "456", 12, "a"].asText()
node get "123".asInt()
```

read value:

```kotlin
node.asInt() / asText() / asBoolean() / ...
node.asList() // ArrayList<Any>
node.asMap() // LinkedHashMap<String, Any>
node.asUUID() // to UUID
node.asMaterial() // to Bukkit Material
node.asType<YourClass>() // to your class
node.asConfigurationSerializable<ItemStack>() // to Bukkit ConfigurationSerializable
node.asBase64Inventory() / asBase64ItemStacks() / asBase64ItemStack()
```

write value:

```kotlin
node.put("key", 1 / "123" / true / ...)
node.put("key", listOf(1, 2, 3))
node.put("key", mapOf("a" to 1, "b" to 2))
node.put("key", YourClass())
node.put("key", ItemStack(Material.APPLE, 1))
node.putBase64("key", Inventory / ItemStack / Array<ItemStack>)
```

## Reference

- [Jackson Core](https://github.com/FasterXML/jackson-core)
- [Jackson Databind](https://github.com/FasterXML/jackson-databind)
- [Jackson YAML](https://github.com/FasterXML/jackson-dataformats-text/tree/master/yaml)
- [Jackson XML](https://github.com/FasterXML/jackson-dataformat-xml)
- [Jackson CSV](https://github.com/FasterXML/jackson-dataformats-text/tree/master/csv)
- [Jackson Properties](https://github.com/FasterXML/jackson-dataformats-text/tree/master/properties)
- [Jackson TOML](https://github.com/FasterXML/jackson-dataformats-text/tree/2.13/toml)
