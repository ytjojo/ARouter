package com.alibaba.android.arouter.register.launch

import cn.jailedbird.arouter_gradle_plugin.utils.InjectUtils
import com.alibaba.android.arouter.register.agp8.GenerateRegisterCodeTask
import com.alibaba.android.arouter.register.agp8.RegisterClassVisitorFactory
import com.alibaba.android.arouter.register.agp8.RegisterPluginParams
import com.alibaba.android.arouter.register.utils.Logger
import com.alibaba.android.arouter.register.utils.ScanSetting
import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * AGP 8.0+ compatible plugin implementation using Instrumentation API
 * This replaces the deprecated Transform API implementation
 */
abstract class PluginLaunch : Plugin<Project> {

    override fun apply(project: Project) {
        val isApp = project.plugins.hasPlugin(AppPlugin::class.java)
        //only application module needs this plugin to generate register code
        if (isApp) {
            Logger.make(project)
            Logger.i("Project enable arouter-register plugin (AGP 8.0+ compatible)")

            // Get Android components extension
            val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

            // Initialize register settings
            val registerList = ArrayList<ScanSetting>()
            registerList.add(ScanSetting("IRouteRoot"))
            registerList.add(ScanSetting("IInterceptorGroup"))
            registerList.add(ScanSetting("IProviderGroup"))
            registerList.add(ScanSetting("ITemplateGroup"))
            registerList.add(ScanSetting("IMultiImplementGroup"))
            InjectUtils.registerList.clear()
            InjectUtils.registerList.addAll(registerList)

            // Register instrumentation for each variant

            androidComponents.onVariants(androidComponents.selector().all()) { variant: ApplicationVariant ->
                if (variant.buildType == "release" || variant.buildType == "debug") {
                    // Register our instrumentation
                    variant.instrumentation.transformClassesWith(
                        RegisterClassVisitorFactory::class.java,
                        InstrumentationScope.ALL
                    ) { params: RegisterPluginParams ->
                        params.getRegisterList().set(registerList)
                    }

                    // Register code generation task
                    val taskName = "generate${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}ARouterRegisterCode"
                    val genTask = project.tasks.register(
                        taskName,
                        GenerateRegisterCodeTask::class.java
                    ) { task: GenerateRegisterCodeTask ->
                        task.getRegisterList().set(registerList)
                        task.getVariantName().set(variant.name)
                    }

                    // https://github.com/android/gradle-recipes
                    variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                        .use(genTask)
                        .toTransform(
                            ScopedArtifact.CLASSES,
                            GenerateRegisterCodeTask::allJars,
                            GenerateRegisterCodeTask::allDirectories,
                            GenerateRegisterCodeTask::output
                        )

                }
            }
        }
    }
}