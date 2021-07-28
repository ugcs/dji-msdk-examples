package com.example.ugcssample.drone

interface LoadProgressCallback {
    open fun onLoadProgressChanged(current: Long, total: Long)
}