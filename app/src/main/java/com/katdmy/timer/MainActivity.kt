package com.katdmy.timer

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.katdmy.timer.data.TimerSettingsSerializer
import com.katdmy.timer.ui.MainViewModel
import com.katdmy.timer.ui.theme.TimerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var tts: TextToSpeech
    var ttsEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    TimerApp(
                        dataStore = timerSettingsDataStore,
                        playWhistle = { context, soundNum -> playWhistle(context, soundNum) },
                        sayRoundNumber = { number -> sayRoundNumber(number) }
                    )
                }
            }
        }

        tts = TextToSpeech(this) {
            tts.language = Locale("en", "US")
            tts.setPitch(1.3f)
            tts.setSpeechRate(0.7f)
            ttsEnabled = true
        }
    }

    private fun playWhistle(context: Context, soundNum: Int) {
        val whistleResId = when (soundNum) {
            0 -> R.raw.short_whistle
            1 -> R.raw.single_whistle
            2 -> R.raw.long_whistle
            else -> { throw IllegalArgumentException("Illegal sound id")}
        }
        val mp = MediaPlayer.create(context, whistleResId)
        mp.setOnCompletionListener { mp.release() }
        mp.start()
    }

    private fun sayRoundNumber(number: Int) {
        if (ttsEnabled)
            tts.speak("/ / / /  Round $number", TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private val Context.timerSettingsDataStore: DataStore<TimerSettings> by dataStore(
        fileName = "timer_settings.pb",
        serializer = TimerSettingsSerializer
    )
}


@Composable
fun TimerApp(
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    dataStore: DataStore<TimerSettings>,
    playWhistle: (Context, Int) -> Unit,
    sayRoundNumber: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    val timerSettings: State<TimerSettings> = dataStore.data.collectAsState(TimerSettings.getDefaultInstance())

    val timerSet = viewModel.timerSet.observeAsState().value ?: false
    val currentRound = viewModel.currentRound.observeAsState().value ?: 0
    val currentTimerTime = viewModel.currentTimerTime.observeAsState().value ?: 0
    val timerName = viewModel.timerName.observeAsState().value ?: ""

    val shortWhistle = viewModel.shortWhistle.observeAsState().value ?: 0
    val singleWhistle = viewModel.singleWhistle.observeAsState().value ?: 0
    val longWhistle = viewModel.longWhistle.observeAsState().value ?: 0

    ObserveLongWhistle(longWhistle, playWhistle)

    if (timerSet) {
        TimerScreen(
            roundSet = currentRound,
            timeSet = currentTimerTime,
            timerName = timerName,
            onStopClicked = { viewModel.stopTimer() }
        )
        ObserveSounds(shortWhistle, singleWhistle, playWhistle)
        ObserveNewRounds(currentRound, sayRoundNumber)
    } else {
        Settings(
            roundSet = timerSettings.value.roundSet,
            updateRoundSet = {
                    round: Int -> scope.launch {
                        dataStore.updateData {
                                preferences -> preferences.toBuilder().setRoundSet(round).build()
                        }
                    }
            },
            workSecondsSet = timerSettings.value.workSecondsSet,
            updateWorkSet = {
                    work: Int -> scope.launch {
                        dataStore.updateData {
                                preferences -> preferences.toBuilder().setWorkSecondsSet(work).build()
                        }
                    }
            },
            restSecondsSet = timerSettings.value.restSecondsSet,
            updateRestSet = {
                    rest: Int -> scope.launch {
                        dataStore.updateData {
                                preferences -> preferences.toBuilder().setRestSecondsSet(rest).build()
                        }
                    }
            },
            onStartClicked = {
                viewModel.startTimer(
                    timerSettings.value.roundSet,
                    timerSettings.value.workSecondsSet,
                    timerSettings.value.restSecondsSet
                )
            }
        )
    }
}

@Composable
fun ObserveSounds(
    shortWhistle: Long,
    singleWhistle: Long,
    playWhistle: (Context, Int) -> Unit
) {
    val context = LocalContext.current
    if (shortWhistle > 0) {
        LaunchedEffect(shortWhistle) {
            playWhistle(context, 0)
        }
    }
    if (singleWhistle > 0) {
        LaunchedEffect(singleWhistle) {
            playWhistle(context, 1)
        }
    }
}

@Composable
fun ObserveLongWhistle(
    longWhistle: Long,
    playWhistle: (Context, Int) -> Unit
) {
    val context = LocalContext.current
    if (longWhistle > 0) {
        LaunchedEffect(longWhistle) {
            playWhistle(context, 2)
        }
    }
}

@Composable
fun ObserveNewRounds(
    currentRound: Int,
    sayRoundNumber: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    if (currentRound > 1) {
        LaunchedEffect(currentRound) {
            scope.launch {
                delay(1000)
                sayRoundNumber(currentRound)
            }
        }
    }
}


@Composable
fun SettingsHeader() {
    Text(text = "Interval Timer", fontSize = 36.sp)
}

@Composable
fun SetsHeader() {
    Text(text = "SETS")
}

@Composable
fun SetsRow(
    roundSet: Int,
    updateRoundSet: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { if (roundSet > 0) updateRoundSet(roundSet - 1) },
            modifier = Modifier) {
            Text(text = "-", fontSize = 36.sp)
        }
        Spacer(
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = roundSet.toString(),
            onValueChange = { updateRoundSet(it.toInt()) },
            maxLines = 1,
            textStyle = TextStyle(fontSize = 36.sp, textAlign = TextAlign.Center),
            modifier = Modifier.width(80.dp)
        )
        Spacer(
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = {  updateRoundSet(roundSet + 1) },
            modifier = Modifier) {
            Text(text = "+", fontSize = 36.sp)
        }
    }
}

@Composable
fun Sets(roundSet: Int,
         updateRoundSet: (Int) -> Unit,
         modifier: Modifier = Modifier
) {
    Column(modifier = Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SetsHeader()
        SetsRow(
            roundSet = roundSet,
            updateRoundSet = updateRoundSet,
            modifier = modifier
        )
    }
}

@Composable
fun WorkHeader() {
    Text(text = "WORK")
}

@Composable
fun WorkRow(
    workSet: Int,
    updateWorkSet: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { if (workSet > 0) updateWorkSet(workSet - 1) },
            modifier = Modifier) {
            Text(text = "-", fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = "%02d".format(workSet / 60),
            onValueChange = { updateWorkSet(it.toInt() * 60 + workSet % 60) },
            maxLines = 1,
            textStyle = TextStyle(fontSize = 36.sp, textAlign = TextAlign.End),
            modifier = Modifier.width(80.dp))
        Text(":", modifier = Modifier.padding(2.dp), fontSize = 36.sp)
        OutlinedTextField(
            value = "%02d".format(workSet % 60),
            onValueChange = { updateWorkSet((workSet / 60) * 60 + it.toInt()) },
            maxLines = 1,
            textStyle = TextStyle(fontSize = 36.sp, textAlign = TextAlign.Start),
            modifier = Modifier.width(80.dp))
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { updateWorkSet(workSet + 1) },
            modifier = Modifier) {
            Text(text = "+", fontSize = 36.sp)
        }
    }
}

@Composable
fun Work(
    workSet: Int,
    updateWorkSet: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        WorkHeader()
        WorkRow(workSet, updateWorkSet, modifier)
    }
}

@Composable
fun RestHeader() {
    Text(text = "REST")
}

@Composable
fun RestRow(
    restSet: Int,
    updateRestSet: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { if (restSet > 0) updateRestSet(restSet - 1) },
            modifier = Modifier) {
            Text(text = "-", fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = "%02d".format(restSet / 60),
            onValueChange = { updateRestSet(it.toInt() * 60 + restSet % 60) },
            maxLines = 1,
            textStyle = TextStyle(fontSize = 36.sp, textAlign = TextAlign.End),
            modifier = Modifier.width(80.dp))
        Text(":", modifier = Modifier.padding(2.dp), fontSize = 36.sp)
        OutlinedTextField(
            value = "%02d".format(restSet % 60),
            onValueChange = { updateRestSet((restSet / 60) * 60 + it.toInt()) },
            maxLines = 1,
            textStyle = TextStyle(fontSize = 36.sp, textAlign = TextAlign.Start),
            modifier = Modifier.width(80.dp))
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { updateRestSet(restSet + 1) },
            modifier = Modifier) {
            Text(text = "+", fontSize = 36.sp)
        }
    }
}

@Composable
fun Rest(
    restSet: Int,
    updateRestSet: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        RestHeader()
        RestRow(restSet, updateRestSet, modifier)
    }
}

@Composable
fun StartButton(modifier: Modifier = Modifier, onStartClicked: () -> Unit) {
    Button(modifier = modifier, onClick = { onStartClicked() }) {
        Text("START", fontSize = 24.sp)
    }
}

@Composable
fun StartRow(modifier: Modifier = Modifier, onStartClicked: () -> Unit) {
    Row(modifier = modifier) {
        Spacer(modifier = Modifier.weight(1f))
        StartButton(onStartClicked = onStartClicked)
    }
}

@Composable
fun Settings(
    roundSet: Int,
    updateRoundSet: (Int) -> Unit,
    workSecondsSet: Int,
    updateWorkSet: (Int) -> Unit,
    restSecondsSet: Int,
    updateRestSet: (Int) -> Unit,
    onStartClicked: () -> Unit,
    modifier: Modifier = Modifier
    ) {
    Column(modifier = modifier.padding(30.dp)) {
        SettingsHeader()
        Spacer(modifier = Modifier.padding(20.dp))
        Sets(roundSet, updateRoundSet)
        Spacer(modifier = Modifier.padding(10.dp))
        Work(workSecondsSet, updateWorkSet)
        Spacer(modifier = Modifier.padding(10.dp))
        Rest(restSecondsSet, updateRestSet)
        Spacer(modifier = Modifier.padding(20.dp))
        StartRow(modifier = Modifier.align(Alignment.End), onStartClicked)
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsPreview() {
    TimerTheme {
        Settings(6, {}, 30, {}, 30, {}, {})
    }
}


@Composable
fun TimerScreen(
    roundSet: Int,
    timeSet: Int,
    timerName: String,
    onStopClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (timerName) {
        "Prepare" -> Color(0xFF66BB6A)
        "Work" -> Color(0xFFEF5350)
        "Rest" -> Color(0xFF42A5F5)
        else -> MaterialTheme.colors.background
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column {
            Spacer(modifier = Modifier
                .weight(1f))
            Text(text = "Round $roundSet",
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 64.sp)
            Text(text = timerName,
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 64.sp)
            Spacer(modifier = Modifier
                .weight(1f))
            Text(text = "${"%02d".format(timeSet / 60)} : ${"%02d".format(timeSet % 60)}",
                modifier = Modifier
                    .weight(6f)
                    .fillMaxWidth(),
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 110.sp)
            Button(onClick = { onStopClicked() }, modifier = Modifier
                .weight(3f)
                .fillMaxWidth()) { Text(text = "STOP", fontSize = 36.sp) }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TimerScreenPreview() {
    TimerTheme {
        TimerScreen(roundSet = 6, timeSet = 30, timerName = "Prepare", onStopClicked = {})
    }
}
