package earth.levi.dotenv.util

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

object Log {

    fun debug(project: Project, message: String) {
        project.logger.log(LogLevel.DEBUG, message)
    }

    fun info(project: Project, message: String) {
        project.logger.log(LogLevel.INFO, message)
    }

}

fun Project.logDebug(message: String) {
    Log.debug(this, message)
}

fun Project.logInfo(message: String) {
    Log.info(this, message)
}