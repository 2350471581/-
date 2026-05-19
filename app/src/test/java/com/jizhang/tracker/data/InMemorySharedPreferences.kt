package com.jizhang.tracker.data

import android.content.SharedPreferences

class InMemorySharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> = map.toMap()
    override fun getString(key: String, defValue: String?) = map[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValue: MutableSet<String>?) = map[key] as? MutableSet<String> ?: defValue
    override fun getInt(key: String, defValue: Int) = (map[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long) = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float) = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String) = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = InMemoryEditor()
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) { listeners.add(l) }
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) { listeners.remove(l) }

    inner class InMemoryEditor : SharedPreferences.Editor {
        private val tempMap = mutableMapOf<String, Any?>()
        private val removeSet = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?) = apply { tempMap[key] = value }
        override fun putStringSet(key: String, value: MutableSet<String>?) = apply { tempMap[key] = value }
        override fun putInt(key: String, value: Int) = apply { tempMap[key] = value }
        override fun putLong(key: String, value: Long) = apply { tempMap[key] = value }
        override fun putFloat(key: String, value: Float) = apply { tempMap[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { tempMap[key] = value }
        override fun remove(key: String) = apply { removeSet.add(key) }
        override fun clear() = apply { clearAll = true; tempMap.clear(); removeSet.clear() }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            val changedKeys = mutableSetOf<String>()
            if (clearAll) {
                changedKeys.addAll(map.keys)
                map.clear()
                clearAll = false
            }
            changedKeys.addAll(removeSet)
            removeSet.forEach { map.remove(it) }
            map.putAll(tempMap)
            changedKeys.addAll(tempMap.keys)
            listeners.forEach { l ->
                changedKeys.forEach { k ->
                    l.onSharedPreferenceChanged(this@InMemorySharedPreferences, k)
                }
            }
        }
    }
}
