package com.example.ugcssample.utils

import java.util.*

object ArrayUtils {
    fun isEmpty(list: MutableList<*>?): Boolean {
        return list == null || list.isEmpty()
    }

    fun <T> copy(src: MutableList<T?>?): MutableList<T?>? {
        if (src == null) return null
        val retVal: MutableList<T?> = ArrayList(src.size)
        for (element in src) {
            retVal.add(element)
        }
        return retVal
    }

    fun copy(src: DoubleArray?): DoubleArray? {
        if (src == null) return null
        val retVal = DoubleArray(src.size)
        for (i in src.indices) {
            retVal[i] = src[i]
        }
        return retVal
    }

    fun newDouble(size: Int, `val`: Double): DoubleArray? {
        val retVal = DoubleArray(size)
        for (i in 0 until size) {
            retVal[i] = `val`
        }
        return retVal
    }

    fun newBoolean(size: Int, `val`: Boolean): BooleanArray? {
        val retVal = BooleanArray(size)
        for (i in 0 until size) {
            retVal[i] = `val`
        }
        return retVal
    }

    fun setBoolean(retVal: BooleanArray?, value: Boolean) {
        if (retVal?.indices != null) {
            for (i in retVal.indices) {
                retVal[i] = value
            }
        }
    }

    fun findMax(values: DoubleArray?): Double? {
        var d: Double? = null
        if (values != null) {
            for (v in values) {
                if (d == null || d < v) d = v
            }
        }
        return d
    }

    fun findMin(values: DoubleArray?): Double? {
        var d: Double? = null
        if (values != null) {
            for (v in values) {
                if (d == null || d > v) d = v
            }
        }
        return d
    }

    fun clone(src: ArrayList<Double?>?): MutableList<Double?>? {
        return if (src == null) {
            null
        } else ArrayList(src)
    }

    fun copy(origin: DoubleArray?, target: DoubleArray?, valueToAdd: Double) {
        if (origin?.indices != null) {
            for (i in origin.indices) {
                target?.set(i, origin[i] + valueToAdd)
            }
        }
    }
}