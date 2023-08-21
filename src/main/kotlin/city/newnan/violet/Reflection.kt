package city.newnan.violet

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method


object Reflection {
    private val minecraftClassCache: MutableMap<String, Class<*>?> = HashMap()
    private val bukkitClassCache: MutableMap<String, Class<*>?> = HashMap()
    private val methodCache: MutableMap<Class<*>, MutableMap<Pair<String, Array<out Class<*>?>>, Method?>> = HashMap()
    private val declaredMethodCache: MutableMap<Class<*>, MutableMap<Pair<String, Array<out Class<*>?>>, Method?>> = HashMap()
    private val fieldCache: MutableMap<Class<*>, MutableMap<String, Field?>> = HashMap()
    private val declaredFieldCache: MutableMap<Class<*>, MutableMap<String, Field?>> = HashMap()
    private val foundFields: MutableMap<Class<*>, MutableMap<Class<*>, Field?>> = HashMap()
    private val version by lazy { Bukkit.getServer().javaClass.getPackage().name.substring(23) }

    @Synchronized
    fun getBukkitClass(className: String): Class<*>? {
        return bukkitClassCache.getOrPut(className) {
            val clazzName = "org.bukkit.craftbukkit.$version.$className"
            try {
                Class.forName(clazzName)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
        }
    }

    @Synchronized
    fun getMinecraftClass(className: String): Class<*>? {
        return minecraftClassCache.getOrPut(className) {
            val clazzName = "net.minecraft.server.$version.$className"
            try {
                Class.forName(clazzName)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getConnection(player: Player): Any? {
        val getHandleMethod = getMethod(player.javaClass, "getHandle")
        if (getHandleMethod != null) try {
            val nmsPlayer = getHandleMethod.invoke(player)
            val playerConField = getField(nmsPlayer.javaClass, "playerConnection")!!
            return playerConField[nmsPlayer]
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getConstructor(clazz: Class<*>, vararg params: Class<*>?): Constructor<*>? {
        return try {
            clazz.getConstructor(*params)
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    fun getMethod(clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? =
        getMethod(false, clazz, methodName, *params)

    @Synchronized
    fun getMethod(silent: Boolean, clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? =
        methodCache.getOrPut(clazz) { mutableMapOf() }.getOrPut(methodName to params) {
            try {
                val method = clazz.getMethod(methodName, *params)
                method.isAccessible = true
                method
            } catch (e: Exception) {
                if (!silent) e.printStackTrace()
                null
            }
        }

    fun getDeclaredMethod(clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? =
        getDeclaredMethod(false, clazz, methodName, *params)

    @Synchronized
    fun getDeclaredMethod(silent: Boolean, clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? =
        declaredMethodCache.getOrPut(clazz) { mutableMapOf() }.getOrPut(methodName to params) {
            try {
                val method = clazz.getDeclaredMethod(methodName, *params)
                method.isAccessible = true
                method
            } catch (e: Exception) {
                if (!silent) e.printStackTrace()
                null
            }
        }

    @Synchronized
    fun getField(clazz: Class<*>, fieldName: String): Field? =
        fieldCache.getOrPut(clazz) { mutableMapOf() }.getOrPut(fieldName) {
            try {
                val field = clazz.getField(fieldName)
                field.isAccessible = true
                field
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    @Synchronized
    fun getDeclaredField(clazz: Class<*>, fieldName: String): Field? =
        declaredFieldCache.getOrPut(clazz) { mutableMapOf() }.getOrPut(fieldName) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    @Synchronized
    fun findField(clazz: Class<*>, type: Class<*>): Field? =
        foundFields.getOrPut(clazz) { mutableMapOf() }.getOrPut(type) {
            try {
                val allFields = mutableListOf<Field>().apply {
                    addAll(clazz.fields)
                    addAll(clazz.declaredFields)
                }
                allFields.find { it.type == type }?.also { it.isAccessible = true }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}