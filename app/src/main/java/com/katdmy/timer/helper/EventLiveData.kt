package com.katdmy.timer.helper

import androidx.lifecycle.LiveData

class EventLiveData<T> : LiveData<T>() {

    fun triggerEvent(event: T) {
        value = event
    }

}