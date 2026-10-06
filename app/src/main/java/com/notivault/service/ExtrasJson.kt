package com.notivault.service

import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject

/** Dumps a notification's extras Bundle to JSON. Text/numbers are kept; images and binary objects become "<TypeName>". */
object ExtrasJson {

    @Suppress("DEPRECATION")
    fun toJson(bundle: Bundle, depth: Int = 0): JSONObject {
        val obj = JSONObject()
        val keys = try { bundle.keySet().sorted() } catch (t: Throwable) {
            return obj.put("_error", "unreadable bundle: ${t.javaClass.simpleName}")
        }
        for (key in keys) {
            val value = try { bundle.get(key) } catch (t: Throwable) { "<unreadable: ${t.javaClass.simpleName}>" }
            try {
                obj.put(key, toValue(value, depth))
            } catch (t: Throwable) {
                obj.put(key, value.toString())
            }
        }
        return obj
    }

    private fun toValue(v: Any?, depth: Int): Any = when (v) {
        null -> JSONObject.NULL
        is CharSequence -> v.toString()
        is Boolean -> v
        is Int -> v
        is Long -> v
        is Double -> if (v.isFinite()) v else v.toString()
        is Float -> if (v.isFinite()) v.toDouble() else v.toString()
        is Short -> v.toInt()
        is Byte -> v.toInt()
        is Char -> v.toString()
        is Bundle -> if (depth < 5) toJson(v, depth + 1) else "<Bundle>"
        is Array<*> -> JSONArray().apply { v.forEach { put(toValue(it, depth + 1)) } }
        is Collection<*> -> JSONArray().apply { v.forEach { put(toValue(it, depth + 1)) } }
        is IntArray -> JSONArray(v.toList())
        is LongArray -> JSONArray(v.toList())
        is BooleanArray -> JSONArray(v.toList())
        is DoubleArray -> JSONArray(v.filter { it.isFinite() })
        is FloatArray -> JSONArray(v.filter { it.isFinite() }.map { it.toDouble() })
        else -> "<${v.javaClass.simpleName}>"
    }
}
