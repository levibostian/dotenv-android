package earth.levi.dotenv.util

import earth.levi.dotenv.DotEnvPlugin
import it.unimi.dsi.fastutil.io.TextIO.BUFFER_SIZE
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.gradle.api.Project
import java.io.*
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

object CliUtil {

    private const val CLI_DOWNLOAD_MAC_64 = "https://github.com/levibostian/dotenv/releases/download/{version}/dotenv_{version}_Darwin_x86_64.tar.gz"
    private const val CLI_DOWNLOAD_LINUX_arm64 = "https://github.com/levibostian/dotenv/releases/download/{version}/dotenv_{version}_Linux_arm64.tar.gz"
    private const val CLI_DOWNLOAD_LINUX_i386 = "https://github.com/levibostian/dotenv/releases/download/{version}/dotenv_{version}_Linux_i386.tar.gz"
    private const val CLI_DOWNLOAD_LINUX_64 = "https://github.com/levibostian/dotenv/releases/download/{version}/dotenv_{version}_Linux_x86_64.tar.gz"

    fun doesCliExist(project: Project): Boolean {
        return getCliVersion(project) != null
    }

    fun isCliVersionCompatible(project: Project): Boolean {
        if (!doesCliExist(project)) return false

        // takes X.Y.Z and creates an integer that we can compare
        val getCompareString: (String) -> Int = { versionString ->
            val versionStringSplit = versionString.split(".")
            val major = versionStringSplit[0].toInt()
            val minor = versionStringSplit[1].toInt()
            val patch = versionStringSplit[2].toInt()

            (major * 1000000) + (minor * 100000) + patch
        }

        val cliVersionInstalled = getCliVersion(project)!!
        Log.debug(project, "---Asserting CLI version compatible.")
        Log.debug(project, "Installed CLI version: $cliVersionInstalled")

        val currentInstalledVersionCompareString = getCompareString(cliVersionInstalled)
        val minVersionInstalled = getCompareString(DotEnvPlugin.COMPATIBLE_CLI_VERSION)
        val maxVersionInstalled = getCompareString("${DotEnvPlugin.NEXT_MAJOR_CLI_VERSION}.0.0")
        Log.debug(project, "Version values to compare. Current installed: $currentInstalledVersionCompareString, min: $minVersionInstalled, max: $maxVersionInstalled")

        return currentInstalledVersionCompareString >= minVersionInstalled && currentInstalledVersionCompareString < maxVersionInstalled
    }

    private fun getCliVersion(project: Project): String? {
        project.logDebug("Getting CLI version....")

        return try {
            val byteOutputStream = ByteArrayOutputStream()

            project.exec {
                it.standardOutput = byteOutputStream
                it.isIgnoreExitValue = false
                it.executable = project.dotenvCliBinPath()
                it.args = listOf("version")
            }.assertNormalExitValue()

            val cliVersion = byteOutputStream.toString("UTF-8").trim() // trim to remove newline at the end

            project.logDebug("CLI version does exist: $cliVersion")

            cliVersion
        } catch (error: Exception) {
            project.logDebug("Could not get CLI version. Must not be installed.")

            null
        }
    }

    fun downloadCliBin(project: Project) {
        val osName = System.getProperty("os.name").toLowerCase()
        val isWindows = osName.contains("windows")
        val isMac = osName.contains("mac")
        val isLinux = !isWindows && !isMac // yes, there are more OSs out there but we are assuming you are only using 1 of these 3 options

        val cpuType = System.getProperty("os.arch")
        val is64BitCpu = cpuType == "x86_64" || cpuType == "amd64"
        val is32BitCpu = cpuType == "x86" || cpuType == "386" || cpuType == "i386"
        val isArm64Cpu = cpuType == "arm64"

        val downloadBinaryPath = project.buildDir.resolve("dotenv").absolutePath

        project.logInfo("--Downloading binary for local execution. ")
        project.logDebug("downloading to absolute path: $downloadBinaryPath")

        val getUnsupportedBinaryException: () -> RuntimeException = {
            RuntimeException("It looks like the Gradle plugin does not support your computer. Please, file an issue: ${DotEnvPlugin.BUG_REPORT_FILE_URL} with the full stacktrace. OS name: $osName, Arch: $cpuType")
        }

        if (isWindows) {
            project.logDebug("Detected OS: Windows")

            println("It appears you are using Windows as your operating system. Sorry, but at this time dotenv-android requires that you manually install a dependency in order to use this Gradle plugin.")
            throw RuntimeException("Follow the install instructions: ${DotEnvPlugin.DOTENV_CLI_MANUAL_INSTALL_INSTRUCTIONS} and try running the plugin again.")
        }

        if (isMac) {
            project.logDebug("Detected OS: Mac")

            val baseDownloadUrl = (if (is64BitCpu) CLI_DOWNLOAD_MAC_64 else null)
                    ?: throw getUnsupportedBinaryException()

            val downloadPath = baseDownloadUrl.replace("{version}", DotEnvPlugin.COMPATIBLE_CLI_VERSION)
            project.logDebug("binary download URL: $downloadPath")

            downloadFile(downloadPath, downloadBinaryPath)
        }

        if (isLinux) {
            project.logDebug("Detected OS: Linux")

            val baseDownloadUrl = (if (is64BitCpu) CLI_DOWNLOAD_LINUX_64 else if (is32BitCpu) CLI_DOWNLOAD_LINUX_i386 else if (isArm64Cpu) CLI_DOWNLOAD_LINUX_arm64 else null)
                    ?: throw getUnsupportedBinaryException()

            val downloadPath = baseDownloadUrl.replace("{version}", DotEnvPlugin.COMPATIBLE_CLI_VERSION)
            project.logDebug("binary download URL: $downloadPath")

            downloadFile(downloadPath, downloadBinaryPath)
        }

        project.logDebug("binary downloaded successfully")

        // before we end function, lets test to make sure the CLI is ready to execute. make sure it exists and permissions allow us to execute it.
        val localCliFile = File(project.dotenvCliLocalBinPath())
        if (!localCliFile.exists()) {
            throw RuntimeException("There is a bug with the gradle plugin. Please, file an issue ${DotEnvPlugin.BUG_REPORT_FILE_URL} with this full stacktrace.")
        }
        Files.setPosixFilePermissions(localCliFile.toPath(), PosixFilePermissions.fromString("rwxr-x--x"))
    }

    /**
     * GitHub releases are in .tar.gz so we are using apache-commons library to untar and ungzip while we are downloading.
     *
     * After complete, we should see a "dotenv" directory get created with files inside including our binary.
     */
    private fun downloadFile(url: String, path: String) {
        val inputStream = BufferedInputStream(URL(url).openStream())
        val tarIn = TarArchiveInputStream(GzipCompressorInputStream(inputStream))

        lateinit var entry: TarArchiveEntry
        while (tarIn.nextTarEntry?.also { entry = it } != null) {
            if (entry.isDirectory) { // If the entry is a directory, create the directory.
                val file = File(path).resolve(entry.name)
                if (!file.mkdirs()) {
                    throw RuntimeException("Unable to create directory ${file.absolutePath}")
                }
            } else {
                var count: Int
                val data = ByteArray(BUFFER_SIZE)
                val fos = FileOutputStream(File(path).resolve(entry.name), false)
                BufferedOutputStream(fos, BUFFER_SIZE).use { dest ->
                    while (tarIn.read(data, 0, BUFFER_SIZE).also { count = it } != -1) {
                        dest.write(data, 0, count)
                    }
                }
            }
        }
    }

}

/**
 * Local binary CLI file to execute is: androidApp/app/build/dotenv/dotenv
 *
 * This is because we download the CLI from github actions into a tar.gz when we extract it, it's in the dotenv/ directory.
 */
fun Project.dotenvCliLocalBinPath(): String = buildDir.resolve("dotenv").resolve("dotenv").absolutePath

fun Project.dotenvCliBinPath(): String {
    return if (File(dotenvCliLocalBinPath()).exists()) dotenvCliLocalBinPath()
    else "dotenv"
}