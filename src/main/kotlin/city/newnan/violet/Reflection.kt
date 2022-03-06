package city.newnan.violet

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*


object Reflection {
    private val versionString: String? = null
    private val loadedNMSClasses: MutableMap<String, Class<*>?> = HashMap()
    private val loadedOBCClasses: MutableMap<String, Class<*>?> = HashMap()
    private val loadedMethods: MutableMap<Class<*>, MutableMap<String, Method?>> = HashMap()
    private val loadedDeclaredMethods: MutableMap<Class<*>, MutableMap<String, Method?>> = HashMap()
    private val loadedFields: MutableMap<Class<*>, MutableMap<String, Field?>> = HashMap()
    private val loadedDeclaredFields: MutableMap<Class<*>, MutableMap<String, Field?>> = HashMap()
    private val foundFields: MutableMap<Class<*>, MutableMap<Class<*>, Field?>> = HashMap()
    val version: String
        get() = Bukkit.getServer().javaClass.getPackage().name.substring(23)

    @Synchronized
    fun getOBC(obcClassName: String): Class<*>? {
        val clazz: Class<*>
        if (loadedOBCClasses.containsKey(obcClassName)) return loadedOBCClasses[obcClassName]
        val clazzName = "org.bukkit.craftbukkit.$version.$obcClassName"
        try {
            clazz = Class.forName(clazzName)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            loadedOBCClasses[obcClassName] = null
            return null
        }
        loadedOBCClasses[obcClassName] = clazz
        return clazz
    }

    fun getNMS(nmsClassName: String): Class<*>? {
        val clazz: Class<*>
        if (loadedNMSClasses.containsKey(nmsClassName)) return loadedNMSClasses[nmsClassName]
        val clazzName = "net.minecraft.server.$version.$nmsClassName"
        clazz = try {
            Class.forName(clazzName)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            return loadedNMSClasses.put(nmsClassName, null)
        }
        loadedNMSClasses[nmsClassName] = clazz
        return clazz
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

    fun getMethod(clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? {
        return getMethod(false, clazz, methodName, *params)
    }

    fun getMethod(silent: Boolean, clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? {
        if (!loadedMethods.containsKey(clazz)) loadedMethods[clazz] = HashMap()
        val methods = loadedMethods[clazz]!!
        return if (methods.containsKey(methodName)) methods[methodName] else try {
            val method = clazz.getMethod(methodName, *params)
            methods[methodName] = method
            loadedMethods[clazz] = methods
            method
        } catch (e: Exception) {
            if (!silent) e.printStackTrace()
            methods[methodName] = null
            loadedMethods[clazz] = methods
            null
        }
    }

    fun getDeclaredMethod(clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? {
        return getDeclaredMethod(false, clazz, methodName, *params)
    }

    fun getDeclaredMethod(silent: Boolean, clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? {
        if (!loadedDeclaredMethods.containsKey(clazz)) loadedDeclaredMethods[clazz] = HashMap()
        val methods = loadedDeclaredMethods[clazz]!!
        return if (methods.containsKey(methodName)) methods[methodName] else try {
            val method = clazz.getDeclaredMethod(methodName, *params)
            methods[methodName] = method
            loadedDeclaredMethods[clazz] = methods
            method
        } catch (e: Exception) {
            if (!silent) e.printStackTrace()
            methods[methodName] = null
            loadedDeclaredMethods[clazz] = methods
            null
        }
    }

    fun getField(clazz: Class<*>, fieldName: String): Field? {
        if (!loadedFields.containsKey(clazz)) loadedFields[clazz] = HashMap()
        val fields = loadedFields[clazz]!!
        return if (fields.containsKey(fieldName)) fields[fieldName] else try {
            val field = clazz.getField(fieldName)
            fields[fieldName] = field
            loadedFields[clazz] = fields
            field
        } catch (e: Exception) {
            e.printStackTrace()
            fields[fieldName] = null
            loadedFields[clazz] = fields
            null
        }
    }

    fun getDeclaredField(clazz: Class<*>, fieldName: String): Field? {
        if (!loadedDeclaredFields.containsKey(clazz)) loadedDeclaredFields[clazz] = HashMap()
        val fields = loadedDeclaredFields[clazz]!!
        return if (fields.containsKey(fieldName)) fields[fieldName] else try {
            val field = clazz.getDeclaredField(fieldName)
            fields[fieldName] = field
            loadedDeclaredFields[clazz] = fields
            field
        } catch (e: Exception) {
            e.printStackTrace()
            fields[fieldName] = null
            loadedDeclaredFields[clazz] = fields
            null
        }
    }

    fun findField(clazz: Class<*>, type: Class<*>): Field? {
        if (!foundFields.containsKey(clazz)) foundFields[clazz] = HashMap()
        val fields = foundFields[clazz]!!
        if (fields.containsKey(type)) return fields[type]
        try {
            val allFields: MutableList<Field> = ArrayList()
            allFields.addAll(listOf(*clazz.fields))
            allFields.addAll(listOf(*clazz.declaredFields))
            for (f in allFields) {
                if (type == f.type) {
                    fields[type] = f
                    foundFields[clazz] = fields
                    return f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fields[type] = null
            foundFields[clazz] = fields
        }
        return null
    }
}