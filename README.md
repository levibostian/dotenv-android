# dotenv-android

Give access to `.env` environment variables file within your Android projects. 

`dotenv-android` is a simple Gradle plugin that generates a source code file in your Android project. This allows you to reference values in your project's `.env` file. This tool was inspired by [the twelve-factor app](https://12factor.net/config) to make environmental changes in your app simple.

# Getting started

* This Gradle plugin has a dependency that you need to manually install on your machine (In the future, we hope to make this process automatic to avoid this step). Follow the instructions to [download the `dotenv` CLI program](https://github.com/levibostian/dotenv#install) to your machine. 
  
* In your Android app's `build.gradle` file (probably located at `app/build.gradle`), add the dotenv-android plugin:

```groovy
plugins {
  id "earth.levi.dotenv-android"
}
```

You also need to configure the plugin in your `build.gradle` file:

```groovy
dotenv {
    // the package name that is added to the top of your source code file: `import X.Y.Z`
    packageName = "com.foo.bar"
    // the path to the source code in your Android app module. This is probably `src/main/java` but could be something else like `src/main/kotlin`
    sourcePath = "src/main/java/"
}
```

* Create a `.env` file in the root level of your Android app project. 

Here is an example `.env` file:

```
ENABLE_LOGS=true
API_KEY=XXX-YYY-ZZZ
```

* In your Kotlin or Java source code of your Android app, reference `.env` values by using this syntax:
```kotlin
val enableLogs = Env.enableLogs == "true"
Env.apiKey 
```

At first, you will see your IDE (Android Studio) complain to you that it cannot find `Env.enableLogs`. That's ok! Build your Android app and the dotenv Gradle plugin will scan your source code, find the `Env.X` entries, and generate a source code file for you!

## Development 

The easiest way to get started developing this project is to import the root level `build.gradle` file using Intellij CE. This project includes a very slim Android app in the `androidApp/` directory of this project. It's recommended to import the `androidApp/build.gradle` file into Android Studio.

1. Get both projects open on your computer using Intellij and Android Studio.
2. Write code for the Gradle plugin in Intellij. When you're ready to test out the plugin, run this command in the root level of this project:
```
./gradlew clean build publishToMavenLocal
```

Then, perform a Gradle sync in Android Studio for the Android app. 

3. You're now ready to test the plugin inside of Android Studio. You can perform a build of the Android app within Android Studio to quickly test it. 

If you need to debug the plugin, the best way is to enable logging for the Android app compiling.

From the `androidApp/` directory, run the command: `./gradlew :app:generateDebugDotenv --info`. This will enable the log level *info* (there are many other [log levels including --debug](https://docs.gradle.org/current/userguide/logging.html#sec:choosing_a_log_level)). You can read all of your log statements here to debug the plugin. 

## Deployment 

We deploy to the [Gradle Plugin Portal](https://plugins.gradle.org/) to make using the plugin very easy. 

* [Register for an account](https://plugins.gradle.org/user/register) with the Gradle Plugin Portal to get your set of API keys. 

* Create a file on your machine with this format:

```
gradle.publish.key=<key here>
gradle.publish.secret=<secret here>
```

Replace `<key here>` and `<secret here>` with your API key and secret you received after registering with Gradle Plugin Portal. 

* Run `cat /path/to/file/you/made/above/file.txt | base64` and set the output string as a GitHub Actions secret in your GitHub repo. The secret key is `GRADLE_LOGIN_BASE64`. 

* Set another GitHub Action secret `REPO_PUSH_TOKEN` - GitHub personal access token with `repos` scope. This allows CI server to push git commits to the GitHub repository. 

* Done! The semantic-release tool will now deploy your gradle plugin to the portal every time you deploy a new version of the project. 

## Author

* Levi Bostian - [GitHub](https://github.com/levibostian), [Twitter](https://twitter.com/levibostian), [Website/blog](http://levibostian.com)

![Levi Bostian image](https://gravatar.com/avatar/22355580305146b21508c74ff6b44bc5?s=250)

## Contribute

dotenv-android is open for pull requests. Check out the [list of issues](https://github.com/levibostian/dotenv-android/issues) for tasks I am planning on working on. Check them out if you wish to contribute in that way.

**Want to add features?** Before you decide to take a bunch of time and add functionality to the library, please, [create an issue]
(https://github.com/levibostian/dotenv-android/issues/new) stating what you wish to add. This might save you some time in case your purpose does not fit well in the use cases of this project.
