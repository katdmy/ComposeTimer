package com.katdmy.timer.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.katdmy.timer.presentation.theme.TimerTheme
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: MainViewModel by viewModels { MainViewModelFactory(this)}

        setContent {
            TimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    TimerApp(viewModel)
                }
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}


@Composable
fun TimerApp(
    viewModel: MainViewModel
) {
    val initialState by viewModel.initialState.collectAsStateWithLifecycle()
    val currentState by viewModel.currentState.collectAsStateWithLifecycle()

    if (currentState.timerSet) {
        TimerScreen(
            roundSet = currentState.currentRound,
            timeSet = currentState.currentTimerTime,
            timerName = currentState.timerName,
            onStopClicked = { viewModel.stopTimer() }
        )
    } else {
        Settings(
            roundSet = initialState.roundSet,
            updateRoundSet = viewModel::updateRoundSet,
            workSecondsSet = initialState.workSecondsSet,
            updateWorkSet = viewModel::updateWorkSet,
            restSecondsSet = initialState.restSecondsSet,
            updateRestSet = viewModel::updateRestSet,
            onStartClicked = {
                viewModel.startTimer()
            }
        )
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
    // TODO: fix seconds input field - app crashes with intermediate value of "" in Work Field,
    // TODO: input should use replace mode instead of insert mode
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
fun TimerPrepareScreenPreview() {
    TimerTheme {
        TimerScreen(roundSet = 6, timeSet = 10, timerName = "Prepare", onStopClicked = {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TimerWorkScreenPreview() {
    TimerTheme {
        TimerScreen(roundSet = 6, timeSet = 30, timerName = "Work", onStopClicked = {})
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TimerRestScreenPreview() {
    TimerTheme {
        TimerScreen(roundSet = 6, timeSet = 30, timerName = "Rest", onStopClicked = {})
    }
}