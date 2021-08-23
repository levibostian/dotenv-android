package earth.levi.dotenv

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariant
import earth.levi.dotenv.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.SourceTask
import java.io.*
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


open class DotEnvPluginExtension {
    /**
     * Package name for the project.
     * Optional. Default = applicationId in the build.gradle file for Android app.
     *
     * Package name is used for (1) printing a `package` statement at the top of the output file and (2) determining where to build output file in app/build/ directory.
     */
    var packageName: String? = null

    /**
     * Path to get to your source code in your Android app. This is the relative path from your Android app project's build.gradle file directory.
     * Optional. Default = the path that Android Studio uses by default for new Android app projects: src/main/java/
     *
     * This value is important because we don't want to check test files or build files in the `app` module. We only want your app's source code.
     */
    var sourcePath: String = "src/main/java/"
}

class DotEnvPlugin : Plugin<Project> {

    companion object {
        const val PLUGIN_NAME = "dotenv"
        const val COMPATIBLE_CLI_VERSION = "1.1.0" // the minimum version of the CLI tool `dotenv` that this gradle plugin supports.
        const val NEXT_MAJOR_CLI_VERSION = "2" // if `MIN_CLI_VERSION` is `3.X.X`, this value is 4.

        const val DOTENV_CLI_MANUAL_INSTALL_INSTRUCTIONS = "https://github.com/levibostian/dotenv#install"
        const val BUG_REPORT_FILE_URL = "https://github.com/levibostian/dotenv-android/issues/new"
    }

    override fun apply(project: Project) {
        // gets the arguments for the plugin from the project's build.gradle file.
        val extension = project.extensions.create<DotEnvPluginExtension>(PLUGIN_NAME, DotEnvPluginExtension::class.java)

        project.afterEvaluate { project ->
            // We need to create gradle tasks (example: generateDebugDotenv) in order for plugin to be able to run. Plugins create gradle tasks and optionally hook into other gradle tasks (like compiling android app)
            val appExtension = project.extensions.findByType(AppExtension::class.java)
            appExtension?.applicationVariants?.all { buildVariant ->
                setupTasksForVariant(extension, project, buildVariant)
            }
        }
    }

    // takes a build flavor (example "debug") and creates a gradle task to run dotenv.
    private fun setupTasksForVariant(extension: DotEnvPluginExtension, project: Project, variant: BaseVariant) {
        if (extension.packageName == null) extension.packageName = variant.applicationId

        // Variant name is probably "debug" or "release" to match Android build flavor
        val variantNameLowercase = variant.name.decapitalize()
        // Example value: "app/build/generated/source/dotenv/debug" for the "debug" Android build flavor.
        val generatedCodeOutputDir = "${project.buildDir}/generated/source/dotenv/${variantNameLowercase}"

        // Here, we create gradle tasks for our plugin. We need to create tasks for each variant to make sure we are generating code in the correct directory.
        val generateEnvTask = project.task("generate${variant.name.capitalize()}Dotenv") {
            it.doLast {
                // we have our chance to run the task now!
                println("=== dotenv-android plugin running ===")
                println("dotenv-android info:")
                println("dotenv CLI compatible version: >= $COMPATIBLE_CLI_VERSION && < $NEXT_MAJOR_CLI_VERSION")

                File(project.buildDir.absolutePath).resolve("dotenv").mkdirs() // make sure that build dir exists. if we run `gradlew clean` before running task, we might have an exception because build dir does not exist yet.

                val cliInstalled = CliUtil.doesCliExist(project)
                val cliInstalledVersionCompatible = CliUtil.isCliVersionCompatible(project)

                if (!cliInstalled || !cliInstalledVersionCompatible) {
                    println("downloading CLI dependency...")
                    CliUtil.downloadCliBin(project)
                }

                generateEnvForVariant(project, extension, generatedCodeOutputDir)
            }
            it.group = "build"
            it.description = "Generate Env.java file for ${variant.name} build flavor from .env file"
        }

        // This plugin officially supports Android apps compiled with Kotlin code. It has not been tested with only java code.
        // This line below from: https://github.com/Comcast/resourceprovider2/blob/75d1d39e74271a99e8259af46e966fffb3f2e368/src/main/kotlin/com/xfinity/resourceprovider/ResourceProviderPlugin.kt#L91
        variant.registerJavaGeneratingTask(generateEnvTask, File(generatedCodeOutputDir))
        // Hook out task into Kotlin's compile task so we can run our task automatically when app gets compiled.
        val kotlinCompileTask = project.tasks.findByName("compile${variant.name.capitalize()}Kotlin") as? SourceTask
        if (kotlinCompileTask != null) {
            kotlinCompileTask.dependsOn(generateEnvTask)
            val srcSet = project.objects.sourceDirectorySet("dotenv", "dotenv").srcDir(generatedCodeOutputDir)
            kotlinCompileTask.source(srcSet)
        }
    }

    // Actual task that generates the output file. Our gradle task is running.
    private fun generateEnvForVariant(project: Project, extension: DotEnvPluginExtension, generatedCodeOutputDir: String) {
        // output dir is currently "app/build/generated/source/dotenv/debug" but Android Studio will not pick that up.
        // Generated code must be in the directory format: "app/build/generated/source/dotenv/debug/com/foo/bar" to match package name
        // Thanks: https://github.com/square/javapoet/blob/742af18b143dab63e757d9952c7773f60c1af841/src/main/java/com/squareup/javapoet/JavaFile.java#L133-L138
        var path = Paths.get(generatedCodeOutputDir)
        extension.packageName!!.split(".").forEach { path = path.resolve(it) }
        Files.createDirectories(path) // if `./gradlew clean`, for example, is run then the directories need to be re-created.

        val sourcePath = project.projectDir.resolve(extension.sourcePath).absolutePath
        val packageName = extension.packageName
        val dotEnvFilePath = project.rootDir.absolutePath
        project.logDebug("source code path: $sourcePath")
        project.logDebug("package name: $packageName")
        project.logDebug(".env file path: $dotEnvFilePath")

        project.logInfo("Generating Env.java file")

        val outputStream = ByteArrayOutputStream()
        try {
            project.exec {
                it.errorOutput = outputStream
                it.standardOutput = outputStream
                it.workingDir = path.toFile()
                it.executable = project.dotenvCliBinPath()
                it.args = listOf("generate", "java", "--packageName", packageName, "--source", sourcePath, "--env", dotEnvFilePath, "--inputLang", "java,kotlin", "--verbose")
                it.isIgnoreExitValue = false
            }

            outputStream.toString("UTF-8").apply {
                if (isNotBlank()) print(this)
            }
        } catch (err: Exception) {
            // if the CLI has an error, we want to capture that and make it the output of the plugin error. If we don't, the gradle error will be generic like "executing command dotenv failed with non-zero exit code"
            val errOutput = outputStream.toString("UTF-8")
            if (!errOutput.isNullOrEmpty()) throw RuntimeException(errOutput)

            throw err // if there was nothing sent to err/out steam, just throw the error we got. it might be a developer error with the plugin
        }
    }

}
