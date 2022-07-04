package com.katdmy.timer.ui

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.lang.System.currentTimeMillis
import kotlin.math.roundToInt

class MainViewModel: ViewModel() {

    private val _timerSet = MutableLiveData(false)
    val timerSet: LiveData<Boolean> = _timerSet

    private val _currentRound = MutableLiveData(1)
    val currentRound: LiveData<Int> = _currentRound

    private val _currentTimerTime = MutableLiveData(30)
    val currentTimerTime: LiveData<Int> = _currentTimerTime

    private val _timerName = MutableLiveData("Prepare")
    val timerName: LiveData<String> = _timerName

    private val _shortWhistle = MutableLiveData(0L)
    val shortWhistle: LiveData<Long> = _shortWhistle

    private val _singleWhistle = MutableLiveData(0L)
    val singleWhistle: LiveData<Long> = _singleWhistle

    private val _longWhistle = MutableLiveData(0L)
    val longWhistle: LiveData<Long> = _longWhistle

    private val MIN_TIME_FOR_TICK = 5

    private var prepareTimer: CountDownTimer? = null
    private var workTimer: CountDownTimer? = null
    private var restTimer: CountDownTimer? = null

    fun startTimer(
        roundSet: Int,
        workSecondsSet: Int,
        restSecondsSet: Int
    ) {
        _currentRound.value = 1

        restTimer = object : CountDownTimer(restSecondsSet * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timerName.value = "Rest"
                _currentTimerTime.value = (millisUntilFinished / 1000f).roundToInt()
                if (restSecondsSet > MIN_TIME_FOR_TICK)
                    if (millisUntilFinished in 800..3200)
                        _shortWhistle.value = currentTimeMillis()
            }

            override fun onFinish() {
                if (_currentRound.value == roundSet) {
                    _longWhistle.value = currentTimeMillis()
                    _timerSet.value = false
                } else {
                    _singleWhistle.value = currentTimeMillis()
                    _currentRound.value = (_currentRound.value as Int) + 1
                    workTimer!!.start()
                }
            }
        }

        workTimer = object : CountDownTimer(workSecondsSet * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timerName.value = "Work"
                _currentTimerTime.value = (millisUntilFinished / 1000f).roundToInt()
                if (workSecondsSet > MIN_TIME_FOR_TICK)
                    if (millisUntilFinished in 800..3200)
                        _shortWhistle.value = currentTimeMillis()
            }

            override fun onFinish() {
                _singleWhistle.value = currentTimeMillis()
                restTimer!!.start()
            }
        }

        prepareTimer = object : CountDownTimer(10*1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timerName.value = "Prepare"
                _currentTimerTime.value = (millisUntilFinished / 1000f).roundToInt()
                if (millisUntilFinished in 800..3200)
                    _shortWhistle.value = currentTimeMillis()
            }

            override fun onFinish() {
                _singleWhistle.value = currentTimeMillis()
                workTimer!!.start()
            }
        }

        _timerSet.value = true
        prepareTimer!!.start()
    }

    fun stopTimer() {
        prepareTimer?.cancel()
        workTimer?.cancel()
        restTimer?.cancel()
        _timerSet.value = false
    }

}