package com.alibaba.android.arouter.register.agp8

import com.alibaba.android.arouter.register.utils.Logger
import com.alibaba.android.arouter.register.utils.ScanSetting
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * generate register code Into LogisticsCenter.class
 * @author billy.qi email: qiyilike@163.com
 */
class RegisterCodeGenerator private constructor(private val extension: ScanSetting) {

    companion object {
        @JvmStatic
        fun insertInitCodeTo(registerSetting: ScanSetting, fileContainsInitClass: File) {
            if (registerSetting != null && registerSetting.classList.isNotEmpty()) {
                val processor = RegisterCodeGenerator(registerSetting)
                val file = fileContainsInitClass
                if (file.name.endsWith(".jar"))
                    processor.insertInitCodeIntoJarFile(file)
            }
        }
    }

    /**
     * generate code Into jar file
     * @param jarFile the jar file which contains LogisticsCenter.class
     * @return
     */
    private fun insertInitCodeIntoJarFile(jarFile: File): File? {
        if (jarFile.exists()) {
            val optJar = File(jarFile.parent, jarFile.name + ".opt")
            if (optJar.exists())
                optJar.delete()
            val file = JarFile(jarFile)
            val enumeration = file.entries()
            val jarOutputStream = JarOutputStream(FileOutputStream(optJar))

            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement() as JarEntry
                val entryName = jarEntry.name
                val zipEntry = ZipEntry(entryName)
                val inputStream = file.getInputStream(jarEntry)
                jarOutputStream.putNextEntry(zipEntry)
                if (ScanSetting.GENERATE_TO_CLASS_FILE_NAME == entryName) {

                    Logger.i("Insert init code to class >> $entryName")

                    val Bytes = referHackWhenInit(inputStream)
                    jarOutputStream.write(Bytes)
                } else {
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                inputStream.close()
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            file.close()

            if (jarFile.exists()) {
                jarFile.delete()
            }
            optJar.renameTo(jarFile)
        }
        return jarFile
    }

    //refer hack class when object init
    private fun referHackWhenInit(inputStream: InputStream): ByteArray {
        val cr = ClassReader(inputStream)
        val cw = ClassWriter(cr, 0)
        val cv = MyClassVisitor(Opcodes.ASM9, cw, extension)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

    internal class MyClassVisitor(api: Int, cv: ClassVisitor?, private val extension: ScanSetting) :
        ClassVisitor(api, cv) {

        override fun visit(
            version: Int, access: Int, name: String, signature: String?,
            superName: String, Interfaces: Array<String>
        ) {
            super.visit(version, access, name, signature, superName, Interfaces)
        }

        override fun visitMethod(
            access: Int, name: String, desc: String,
            signature: String?, exceptions: Array<String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, desc, signature, exceptions)
            //generate code Into this method
            if (name == ScanSetting.GENERATE_TO_METHOD_NAME) {
                return RouteMethodVisitor(Opcodes.ASM9, mv, extension)
            }
            return mv
        }
    }

    internal class RouteMethodVisitor(
        api: Int,
        mv: MethodVisitor?,
        private val extension: ScanSetting
    ) : MethodVisitor(api, mv) {

        override fun visitInsn(opcode: Int) {
            //generate code before return
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
                extension.classList.forEach { name ->
                    val className = name.replace("/", ".")
                    mv!!.visitLdcInsn(className)//类名
                    // generate invoke register method Into LogisticsCenter.loadRouterMap()
                    mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        ScanSetting.GENERATE_TO_CLASS_NAME,
                        ScanSetting.REGISTER_METHOD_NAME,
                        "(Ljava/lang/String;)V",
                        false
                    )
                }
            }
            super.visitInsn(opcode)
        }

        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            super.visitMaxs(maxStack + 4, maxLocals)
        }
    }
}