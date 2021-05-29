package com.example.morsaapp

import java.util.HashMap
import kotlin.jvm.JvmStatic

class Key<String>(key1: String, key2: String, key3: String) {
    var key1: String? = key1
    var key2: String? = key2
    var key3: String? = key3
    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val key = o as Key<String>
        if (key1 != key.key1) {
            return false
        }
        if (key2 != key.key2) {
            return false
        }
        return key3 == key.key3
    }

    override fun hashCode(): Int {
        var result = if (key1 != null) key1.hashCode() else 0
        result = 31 * result + if (key2 != null) key2.hashCode() else 0
        return result
    }

    override fun toString(): kotlin.String {
        return "[$key1, $key2, $key3]"
    }

}