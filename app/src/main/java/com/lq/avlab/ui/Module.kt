package com.lq.avlab.ui

import com.lq.avlab.AudioRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class Module {

    @Provides
    @Singleton
    fun provideAudioRecordRepository(): AudioRecordRepository {
        return AudioRecordRepository()
    }

}
