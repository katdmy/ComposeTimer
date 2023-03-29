package com.katdmy.timer.presentation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.katdmy.timer.TimerSettings
import com.katdmy.timer.data.TimerSettingsSerializer

class MainViewModelFactory(
    private val activity: ComponentActivity
) : ViewModelProvider.Factory {

    private val Context.timerSettingsDataStore: DataStore<TimerSettings> by dataStore(
        fileName = "timer_settings.pb",
        serializer = TimerSettingsSerializer
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        MainViewModel::class.java -> {
            MainViewModel(
                activity.timerSettingsDataStore,
                activity.application
                )
        }
        else -> throw IllegalArgumentException("$modelClass is not registered ViewModel")
    } as T

}