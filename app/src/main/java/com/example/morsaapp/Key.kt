package com.example.morsaapp

import java.util.HashMap
import kotlin.jvm.JvmStatic

class Key<K1, K2, K3>(key1: K1, key2: K2, key3: K3) {
    var key1: K1? = key1
    var key2: K2? = key2
    var key3: K3 = key3
    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val key = o as Key<*, *, *>
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

    override fun toString(): String {
        return "[$key1, $key2]"
    }

}

internal object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        //  Create a `HashMap` with `Key` as key
        val multiKeyMap: MutableMap<Key<*, *, *>, String> = HashMap()

        // [key1, key2] -> value1
        val k12: Key<*, *, *> = Key<Any?, Any?, Any?>("key1", "key2", "key3")
        multiKeyMap[k12] = "value1"

        // [key3, key4] -> value2
        val k34: Key<*, *, *> = Key<Any?, Any?, Any?>("key4", "key5", "key6")
        multiKeyMap[k34] = "value2"

        // print multikey map
        println(multiKeyMap)

        // print value corresponding to key1 and key2
        println(multiKeyMap[k12])
    }
}