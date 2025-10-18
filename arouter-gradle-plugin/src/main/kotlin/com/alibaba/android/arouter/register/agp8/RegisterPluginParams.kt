package com.alibaba.android.arouter.register.agp8

import com.android.build.api.instrumentation.InstrumentationParameters
import com.alibaba.android.arouter.register.utils.ScanSetting
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input

/**
 * Parameters for the instrumentation
 */
interface RegisterPluginParams : InstrumentationParameters {
    @Input
    fun getRegisterList(): ListProperty<ScanSetting>
}