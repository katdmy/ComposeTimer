package com.katdmy.timer.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.CountDownTimer
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.katdmy.timer.helper.EventLiveData
import com.katdmy.timer.service.SoundService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainViewModel(private val application: Application): AndroidViewModel(application) {

    private val _timerSet = MutableLiveData(false)
    val timerSet: LiveData<Boolean> = _timerSet

    private val _currentRound = MutableLiveData(1)
    val currentRound: LiveData<Int> = _currentRound

    private val _currentTimerTime = MutableLiveData(30)
    val currentTimerTime: LiveData<Int> = _currentTimerTime

    private val _timerName = MutableLiveData("Prepare")
    val timerName: LiveData<String> = _timerName

    private val closingSoundEnded = EventLiveData<Unit>()

    private val MIN_TIME_FOR_TICK = 6

    private var prepareTimer: CountDownTimer? = null
    private var workTimer: CountDownTimer? = null
    private var restTimer: CountDownTimer? = null

    @SuppressLint("StaticFieldLeak")
    private var soundService: SoundService? = null


    init {
        viewModelScope.launch {
            closingSoundEnded.observeForever {
                unbindSoundService()
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            soundService = (iBinder as SoundService.SoundServiceBinder).getService()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            soundService = null
        }
    }

    fun startTimer(
        roundSet: Int,
        workSecondsSet: Int,
        restSecondsSet: Int
    ) {
        _currentRound.value = 1

        restTimer = object : CountDownTimer(restSecondsSet * 1000L, 100) {
            override fun onTick(millisUntilFinished: Long) {
                _timerName.value = "Rest"
                _currentTimerTime.value = (millisUntilFinished / 1000f).roundToInt()
                if (restSecondsSet > MIN_TIME_FOR_TICK)
                    if (millisUntilFinished in 3950..4050 || millisUntilFinished in 2950..3050 || millisUntilFinished in 1950..2050 || millisUntilFinished in 950..1050) {
                        soundService?.playWhistle(0)
                    }
            }

            override fun onFinish() {
                if (_currentRound.value == roundSet) {
                    soundService?.playWhistle(2)
                    _timerSet.value = false
                } else {
                    soundService?.playWhistle(1)
                    _currentRound.value = (_currentRound.value as Int) + 1
                    soundService?.playRoundName(_currentRound.value!!)
                    workTimer!!.start()
                }
            }
        }

        workTimer = object : CountDownTimer(workSecondsSet * 1000L, 100) {
            override fun onTick(millisUntilFinished: Long) {
                _timerName.value = "Work"
                _currentTimerTime.value = (millisUntilFinished / 1000f).roundToInt()
                if (workSecondsSet > MIN_TIME_FOR_TICK)
                    if (millisUntilFinished in 3950..4050 || millisUntilFinished in 2900..3050 || millisUntilFinished in 1950..2050 || millisUntilFinished in 950..1050) {
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
                _timerName.value = "Prepare"
                _currentTimerTime.value = (millisUntilFinished / 1000f).roundToInt()
                if (millisUntilFinished in 3950..4050 || millisUntilFinished in 2900..3050 || millisUntilFinished in 1950..2050 || millisUntilFinished in 950..1050) {
                    soundService?.playWhistle(0)
                }
            }

            override fun onFinish() {
                soundService?.playWhistle(1)
                workTimer!!.start()
            }
        }

        _timerSet.value = true
        prepareTimer!!.start()
        startSoundService()
    }

    fun stopTimer() {
        prepareTimer?.cancel()
        workTimer?.cancel()
        restTimer?.cancel()
        _timerSet.value = false
        application.unbindService(serviceConnection)
    }

    private fun startSoundService() {
        val intent = Intent(application, SoundService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindSoundService() {
        application.unbindService(serviceConnection)
    }

    override fun onCleared() {
        application.unbindService(serviceConnection)
    }
}