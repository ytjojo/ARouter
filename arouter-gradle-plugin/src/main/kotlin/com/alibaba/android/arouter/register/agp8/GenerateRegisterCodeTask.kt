package com.alibaba.android.arouter.register.agp8


import cn.jailedbird.arouter_gradle_plugin.utils.InjectUtils
import cn.jailedbird.arouter_gradle_plugin.utils.ScanUtils
import com.alibaba.android.arouter.register.utils.Logger
import com.alibaba.android.arouter.register.utils.ScanSetting
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.Enumeration
import java.util.jar.JarOutputStream

/**
 * Task to generate register code Into LogisticsCenter.class
 */
abstract class GenerateRegisterCodeTask : DefaultTask() {


    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @Input
    abstract fun getRegisterList(): ListProperty<ScanSetting>

    @Input
    abstract fun getVariantName(): Property<String>

    @TaskAction
    fun taskAction() {
        val start = System.currentTimeMillis()
        val targetList = InjectUtils.registerList
        val leftSlash = File.separator == "/"
        // ARouter plugin scan time spend 11 ms
        println("ARouter plugin scan time spend ${System.currentTimeMillis() - start} ms")

        val startNew = System.currentTimeMillis()
        JarOutputStream(output.asFile.get().outputStream()).use { jarOutput ->
            // Scan directory (Copy and Collection)
            allDirectories.get().forEach { directory ->
                val directoryPath =
                    if (directory.asFile.absolutePath.endsWith(File.separatorChar)) {
                        directory.asFile.absolutePath
                    } else {
                        directory.asFile.absolutePath + File.separatorChar
                    }

                val addedEntries = HashSet<String>()

                // println("Directory is $directoryPath")
                directory.asFile.walk().forEach { file ->
                    if (file.isFile) {
                        val entryName = if (leftSlash) {
                            file.path.substringAfter(directoryPath)
                        } else {
                            file.path.substringAfter(directoryPath).replace(File.separatorChar, '/')
                        }
                        // println("\tDirectory entry name $entryName")
                        if (entryName.isNotEmpty()) {
                            // Use stream to detect register, Take care, stream can only be read once,
                            // So, When Scan and Copy should open different stream;
                            // Copy

                            if (ScanUtils.shouldProcessClass(entryName)) {
                                file.inputStream().use { input ->
                                    ScanUtils.scanClass(input, targetList, false)
                                }
                            }
                            file.inputStream().use { input ->
                                jarOutput.saveEntry(entryName, input, addedEntries)
                            }
                        }
                    }
                }
            }

            // debugCollection(targetList)
            var originInject: ByteArray? = null

            // Scan Jar, Copy & Scan & Code Inject
            val jars = allJars.get().map { it.asFile }
            for (sourceJar in jars) {
                // println("Jar file is $sourceJar")
                val jar = JarFile(sourceJar)
                val entries = jar.entries()
                val addedEntries = HashSet<String>()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    try {
                        // Exclude directory
                        if (entry.isDirectory || entry.name.isEmpty()) {
                            continue
                        }
                        // println("\tJar entry is ${entry.name}")
                        if (entry.name != ScanSetting.GENERATE_TO_CLASS_FILE_NAME) {
                            // Scan and choose
                            if (ScanUtils.shouldProcessClass(entry.name)) {
                                jar.getInputStream(entry).use { inputs ->
                                    ScanUtils.scanClass(inputs, targetList, false)
                                }
                            }
                            // Copy
                            jar.getInputStream(entry).use { input ->
                                jarOutput.saveEntry(entry.name, input, addedEntries)
                            }
                        } else {
                            // Skip
                            // println("Find inject byte code, Skip ${entry.name}")
                            jar.getInputStream(entry).use { inputs ->
                                originInject = inputs.readAllBytes()
                                // println("Find before originInject is ${originInject?.size}")
                            }
                        }
                    } catch (e: Exception) {
                        println("Merge jar error entry:${entry.name}, error is $e ")
                    }
                }
                jar.close()
            }
            debugCollection(targetList)
            // Do inject
            println("Start inject byte code")
            if (originInject == null) { // Check
                error("Can not find ARouter inject point, Do you import ARouter?")
            }
            val resultByteArray = InjectUtils.referHackWhenInit(
                ByteArrayInputStream(originInject), targetList
            )
            jarOutput.saveEntry(
                ScanSetting.GENERATE_TO_CLASS_FILE_NAME,
                ByteArrayInputStream(resultByteArray),
                HashSet<String>()
            )
            println("Inject byte code successful")
        }
        // ARouter plugin scan time spend 11 ms
        println("ARouter plugin inject time spend ${System.currentTimeMillis() - startNew} ms")
    }

    private fun JarOutputStream.saveEntry(
        entryName: String, inputStream: InputStream, addedEntries: HashSet<String>
    ) {
        // 避免重复添加 MANIFEST.MF 条目
        if (addedEntries.contains(entryName)) {
            // 如果已经添加过 MANIFEST.MF，则跳过
            inputStream.close()
            return
        }

        // 记录已添加的条目
        addedEntries.add(entryName)

        this.putNextEntry(JarEntry(entryName))
        IOUtils.copy(inputStream, this)
        this.closeEntry()
    }

    fun generate() {
        Logger.i("Start generating register code for variant: ${getVariantName().get()}")

        val fileContainsInitClass = findLogisticsCenterClass()
        if (fileContainsInitClass != null) {

            InjectUtils.registerList.forEach { ext ->
                Logger.i("Insert register code to file ${fileContainsInitClass.absolutePath}")

                if (ext.classList.isEmpty()) {
                    Logger.e("No class : found for Interface: ${ext.interfaceName}")
                } else {
                    ext.classList.forEach {
                        Logger.i(it)
                    }
                    RegisterCodeGenerator.insertInitCodeTo(ext, fileContainsInitClass)
                }
            }
        } else {
            Logger.e("Could not find LogisticsCenter.class file")
        }
    }

    private fun debugCollection(list: List<ScanSetting>) {
        println("Collect result: size: ${list.size}")
        list.forEach { item ->
            println("${item.interfaceName} : [${item.interfaceName}]")
            item.classList.forEach {
                println("\t $it")
            }
        }
    }

    private fun findLogisticsCenterClass(): File? {
        // Search in jar files
        allJars.get().map { it.asFile }.forEach { jarFile ->
            if (jarFile.name.endsWith(".jar")) {
                try {
                    val file = JarFile(jarFile)
                    val enumeration: Enumeration<*> = file.entries()
                    while (enumeration.hasMoreElements()) {
                        val jarEntry = enumeration.nextElement() as JarEntry
                        val entryName: String = jarEntry.name
                        if (ScanSetting.GENERATE_TO_CLASS_FILE_NAME == entryName) {
                            file.close()
                            return jarFile
                        }
                    }
                    file.close()
                } catch (e: Exception) {
                    Logger.e("Error scanning jar file ${jarFile}: ${e.message}")
                }
            }
        }

        // Search in directories
        allDirectories.get().map { it.asFile }.forEach { dir ->
            val classFile = File(dir, ScanSetting.GENERATE_TO_CLASS_FILE_NAME)
            if (classFile.exists()) {
                return classFile
            }
        }

        return null
    }
}