package com.example.ugcssample.drone.camera

interface LensChangeListener {
    open fun onChange(newLens: Lens?)
}