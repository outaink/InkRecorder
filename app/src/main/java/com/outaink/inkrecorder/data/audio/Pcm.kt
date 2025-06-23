package com.outaink.inkrecorder.data.audio

data class Pcm(
    val pcmData: List<ByteArray> = emptyList()
)

fun Pcm.toWav(
    sampleRate: Int,
) {

}
