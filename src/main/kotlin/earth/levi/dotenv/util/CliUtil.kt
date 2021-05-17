package earth.levi.dotenv.util

import earth.levi.dotenv.DotEnvPlugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*

object CliUtil {

    fun doesCliExist(project: Project): Boolean {
        return getCliVersion(project) != null
    }

    fun assertCliExist(project: Project) {
        if (doesCliExist(project)) return

        println("Sorry! The dotenv-android gradle plugin requires you install a CLI tool before you can use this gradle plugin.")
        println("Install the CLI from the instructions here: ${DotEnvPlugin.DOTENV_CLI_MANUAL_INSTALL_INSTRUCTIONS}")

        throw RuntimeException("Install dotenv CLI and try running plugin again. ${DotEnvPlugin.DOTENV_CLI_MANUAL_INSTALL_INSTRUCTIONS}")
    }

    fun assertCliVersionCompatible(project: Project) {
        // takes X.Y.Z and creates an integer that we can compare
        val getCompareString: (String) -> Int = { versionString ->
            val versionStringSplit = versionString.split(".")
            val major = versionStringSplit[0].toInt()
            val minor = versionStringSplit[1].toInt()
            val patch = versionStringSplit[2].toInt()

            (major * 1000000) + (minor * 100000) + patch
        }

        val cliVersionInstalled = getCliVersion(project)!!
        project.logger.log(LogLevel.DEBUG, "---Asserting CLI version compatible.")
        project.logger.log(LogLevel.DEBUG, "Installed CLI version: $cliVersionInstalled")

        val currentInstalledVersionCompareString = getCompareString(cliVersionInstalled)
        val minVersionInstalled = getCompareString(DotEnvPlugin.COMPATIBLE_CLI_VERSION)
        val maxVersionInstalled = getCompareString("${DotEnvPlugin.NEXT_MAJOR_CLI_VERSION}.0.0")
        project.logger.log(LogLevel.DEBUG, "Version values to compare. Current installed: $currentInstalledVersionCompareString, min: $minVersionInstalled, max: $maxVersionInstalled")

        val isCliVersionCompatible = currentInstalledVersionCompareString >= minVersionInstalled && currentInstalledVersionCompareString < maxVersionInstalled
        project.logger.log(LogLevel.DEBUG, "is cli version compatible: $isCliVersionCompatible")

        if (!isCliVersionCompatible) {
            println("dotenv-android gradle plugin ERROR")
            println("Currently installed version of `dotenv` CLI out-of-date.")
            println("Minimum required version to install: ${DotEnvPlugin.COMPATIBLE_CLI_VERSION} and must be less then ${DotEnvPlugin.NEXT_MAJOR_CLI_VERSION}.0.0 -- Currently installed version: $cliVersionInstalled")

            throw RuntimeException("Currently installed version of dotenv CLI out of date")
        }
    }

    private fun getCliVersion(project: Project): String? {
        return try {
            val byteOutputStream = ByteArrayOutputStream()

            project.exec {
                it.standardOutput = byteOutputStream
                it.isIgnoreExitValue = false
                it.workingDir = project.buildDir
                it.executable = "dotenv"
                it.args = listOf("version")
            }.assertNormalExitValue()

            byteOutputStream.toString(StandardCharsets.UTF_8).trim() // trim to remove newline at the end
        } catch (error: Exception) {
            null
        }
    }

}
