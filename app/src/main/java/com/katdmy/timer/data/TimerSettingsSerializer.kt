package com.katdmy.timer.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.katdmy.timer.TimerSettings
import java.io.InputStream
import java.io.OutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object TimerSettingsSerializer : Serializer<TimerSettings> {
    override val defaultValue: TimerSettings = TimerSettings.newBuilder()
        .setRoundSet(3)
        .setWorkSecondsSet(30)
        .setRestSecondsSet(30)
        .build()

    override suspend fun readFrom(input: InputStream): TimerSettings {
        try {
            return TimerSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: TimerSettings,
        output: OutputStream
    ) = t.writeTo(output)
}