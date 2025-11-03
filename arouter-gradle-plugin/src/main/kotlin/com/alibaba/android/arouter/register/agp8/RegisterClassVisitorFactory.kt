package com.alibaba.android.arouter.register.agp8

import cn.jailedbird.arouter_gradle_plugin.utils.InjectUtils
import com.alibaba.android.arouter.register.utils.Logger
import com.alibaba.android.arouter.register.utils.ScanSetting
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import org.objectweb.asm.ClassVisitor

/**
 * Factory class for creating class visitors for instrumentation
 */
abstract class RegisterClassVisitorFactory : AsmClassVisitorFactory<RegisterPluginParams> {

    override fun isInstrumentable(classData: ClassData): Boolean {
        // We instrument all classes, but focus on scanning router classes
        return true
    }

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor {
        return RegisterClassVisitor( instrumentationContext.apiVersion.get(), nextClassVisitor, parameters.get().getRegisterList().get())
    }

    internal class RegisterClassVisitor(api: Int, classVisitor: ClassVisitor, private val registerList: List<ScanSetting>) : ClassVisitor(api, classVisitor) {
        
        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, Interfaces: Array<out String>?) {
            super.visit(version, access, name, signature, superName, Interfaces)
            // Scan for router classes
            InjectUtils.registerList.forEach { ext ->
                if (ext.interfaceName != null && Interfaces != null) {
                    Interfaces.forEach { itName ->
                        if (itName == ext.interfaceName) {
                            //fix repeated inject init code when Multi-channel packaging
                            if (!ext.classList.contains(name)) {
                                ext.classList.add(name!!)
                                Logger.i("Found class for Interface ${ext.interfaceName}: ${name}" + " total: ${ext.classList.size} " )
                            }
                        }
                    }
                }
            }
        }
    }
}