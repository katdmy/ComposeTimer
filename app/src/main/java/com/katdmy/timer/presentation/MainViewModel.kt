package com.katdmy.timer.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.CountDownTimer
import android.os.IBinder
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katdmy.timer.TimerSettings
import com.katdmy.timer.service.SoundService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.roundToInt

class MainViewModel(
    private val dataStore: DataStore<TimerSettings>,
    private val application: Application
): ViewModel(), SoundService.Listener {

    private val _timerCurrentState = MutableStateFlow(CurrentState())
    val currentState: StateFlow<CurrentState> get() = _timerCurrentState.asStateFlow()

    private val _timerInitialState = MutableStateFlow(InitialState())
    val initialState: StateFlow<InitialState> get() = _timerInitialState.asStateFlow()

    private val minTimeForTick = 5
    private val tickTime = 3

    private var prepareTimer: CountDownTimer? = null
    private var workTimer: CountDownTimer? = null
    private var restTimer: CountDownTimer? = null

    @SuppressLint("StaticFieldLeak")
    private var soundService: SoundService? = null


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            soundService = (iBinder as SoundService.SoundServiceBinder).getService()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            soundService = null
        }
    }

    init {
        viewModelScope.launch {
            dataStore.data
                .catch {
                    if (it is IOException) {
                        it.printStackTrace()
                        emit(TimerSettings.getDefaultInstance())
                    } else {
                        throw it
                    }
                }
                .collect { savedTimerSettings ->
                    _timerInitialState.update { value ->
                        value.copy(
                            roundSet = savedTimerSettings.roundSet,
                            workSecondsSet = savedTimerSettings.workSecondsSet,
                            restSecondsSet = savedTimerSettings.restSecondsSet
                        )
                    }
                }
        }
    }

    fun startTimer() {
        _timerCurrentState.update { value ->
            value.copy(currentRound = 1)
        }

        restTimer = object : CountDownTimer(_timerInitialState.value.restSecondsSet * 1000L, 100) {
            override fun onTick(millisUntilFinished: Long) {
                _timerCurrentState.update { value ->
                    value.copy(timerName = "Rest", currentTimerTime = (millisUntilFinished / 1000f).roundToInt())
                }
                if (_timerInitialState.value.restSecondsSet > minTimeForTick)
                    if (checkIfMillisAreValid(millisUntilFinished)) {
                        soundService?.playWhistle(0)
                    }
            }

            override fun onFinish() {
                if (_timerCurrentState.value.currentRound == _timerInitialState.value.roundSet) {
                    soundService?.playWhistle(2)
                    _timerCurrentState.update { value ->
                        value.copy(timerSet = false)
                    }
                    soundService?.registerListener(this@MainViewModel)
                } else {
                    soundService?.playWhistle(1)
                    _timerCurrentState.update { value ->
                        value.copy(currentRound = _timerCurrentState.value.currentRound + 1)
                    }
                    soundService?.playRoundName(_timerCurrentState.value.currentRound)
                    workTimer!!.start()
                }
            }
        }

        workTimer = object : CountDownTimer(_timerInitialState.value.workSecondsSet * 1000L, 100) {
            override fun onTick(millisUntilFinished: Long) {
                _timerCurrentState.update { value ->
                    value.copy(timerName = "Work", currentTimerTime = (millisUntilFinished / 1000f).roundToInt())
                }
                if (_timerInitialState.value.workSecondsSet > minTimeForTick)
                    if (checkIfMillisAreValid(millisUntilFinished)) {
                        soundService?.playWhistle(0)
                    }
            }

            override fun onFinish() {
                soundService?.playWhistle(1)
                restTimer!!.start()
            }
        }

        prepareTimer = object : CountDownTimer(10*1000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                _timerCurrentState.update { value ->
                    value.copy(timerName = "Prepare", currentTimerTime = (millisUntilFinished / 1000f).roundToInt())
                }
                if (checkIfMillisAreValid(millisUntilFinished)) {
                    soundService?.playWhistle(0)
                }
            }

            override fun onFinish() {
                soundService?.playWhistle(1)
                workTimer!!.start()
            }
        }

        _timerCurrentState.update { value ->
            value.copy(timerSet = true)
        }
        prepareTimer!!.start()
        startSoundService()
    }

    fun stopTimer() {
        prepareTimer?.cancel()
        workTimer?.cancel()
        restTimer?.cancel()
        _timerCurrentState.update { value ->
            value.copy(timerSet = false)
        }
        application.unbindService(serviceConnection)
    }

    private fun startSoundService() {
        val intent = Intent(application, SoundService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        application.unbindService(serviceConnection)
    }

    override fun onClosingSoundEnded() {
        soundService?.unregisterListener(this)
        application.unbindService(serviceConnection)
    }

    private fun checkIfMillisAreValid(millis: Long): Boolean {
        for (currentCheck in 1 .. tickTime) {
            if (millis in currentCheck*1000-50 .. currentCheck*1000+50)
                return true
        }
        return false
    }

    fun updateRoundSet(round: Int) {
        viewModelScope.launch {
            dataStore.updateData {preferences ->
                preferences.toBuilder().setRoundSet(round).build()
            }
        }
    }

    fun updateWorkSet(work: Int) {
        viewModelScope.launch {
            dataStore.updateData { preferences ->
                preferences.toBuilder().setWorkSecondsSet(work).build()
            }
        }
    }

    fun updateRestSet(rest: Int) {
        viewModelScope.launch {
            dataStore.updateData { preferences ->
                preferences.toBuilder().setRestSecondsSet(rest).build()
            }
        }
    }

    data class InitialState(
        val roundSet: Int = 5,
        val workSecondsSet: Int = 30,
        val restSecondsSet: Int = 30
    )

    data class CurrentState(
        val timerSet: Boolean = false,
        val currentRound: Int = 0,
        val currentTimerTime: Int = 0,
        val timerName: String = ""
    )

}