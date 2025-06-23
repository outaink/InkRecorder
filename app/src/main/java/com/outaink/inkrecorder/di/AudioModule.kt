package com.outaink.inkrecorder.di

import com.outaink.inkrecorder.data.audio.AudioStreamer
import com.outaink.inkrecorder.data.audio.AudioStreamerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioStreamer(
        impl: AudioStreamerImpl
    ): AudioStreamer
}