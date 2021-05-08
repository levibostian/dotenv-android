# dotenv-android

Give access to `.env` environment variables file within your Android projects. 

`dotenv-android` is a simple CLI tool you can run on each Gradle build to inject environment variables into your Android app. This tool was inspired by [the twelve-factor app](https://12factor.net/config) to make environmental changes in your app simple. 

*Note: At this time, only Kotlin is supported.*

# Getting started

* Install this tool:

```
gem install dotenv-android
```

* In the root of your Android project, create a `.env` file and store all of the environment variables you wish inside. (Make sure to add this file to your `.gitignore` to avoid checking it into source control!)

* In your Android app's source code, reference environment variables that you want to use:

```kotlin
val apiHost: String = Env.apiHost
```

At first, Android Studio will complain that `Env.apiHost` cannot be found. Don't worry. We will be fixing that. `dotenv-android` CLI crawls your source code looking for `Env.X` requests and generating a `Env.kt` file for you! Anytime you want to use environmental variables, you just need to add it to your source. Super easy. If `dotenv-android` cannot find the variable in your `.env` file, it will throw an error so you can feel confident that if your app compiles, everything will work great. 

* Create a bash script in the root of your project, `generate_env.sh`, with the contents:

```
#!/bin/bash

# You may need to edit the PATH environment variable in order for dotenv-android to execute. It may not be found at first. 
# After you run `gem install dotenv-android`, run `which dotenv-android` to find out where it is located. 
# You may need to add the path to that executable to your script. Android Studio launches with it's own PATH
# that may differ from your system's PATH.
# To see what the PATH variable contains in Android Studio, uncomment the line below and perform a build to run this script:
# /usr/bin/env
export PATH="$PATH:~/.rbenv/shims/"

dotenv-android --source app/src/main/java/com/example/myapplication/ --package PACKAGE_NAME
```

> Tip: You can put the package name in your `.env` file with key `PACKAGE_NAME` as well instead of as a CLI option. 

In your `app/build.gradle` file, add the following to the bottom of the file:
```
task generateEnv(type:Exec) {
    workingDir '../'
    commandLine './generate_env.sh'
}
preBuild.dependsOn generateEnv
```

You have just created a Gradle task that will run every time that you build your app. This will run the `dotenv-android` CLI to generate the `Env.kt` file each time you build! 

* Run a build in Android Studio to run the `dotenv-android` CLI tool. 

* Done! 

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

This gem is setup automatically to deploy to RubyGems on a git tag deployment. 

* Add `RUBYGEMS_KEY` secret to Travis-CI's settings. 
* Make a new git tag, push it up to GitHub. Travis will deploy for you. 

## Author

* Levi Bostian - [GitHub](https://github.com/levibostian), [Twitter](https://twitter.com/levibostian), [Website/blog](http://levibostian.com)

![Levi Bostian image](https://gravatar.com/avatar/22355580305146b21508c74ff6b44bc5?s=250)

## Contribute

dotenv-android is open for pull requests. Check out the [list of issues](https://github.com/levibostian/dotenv-android/issues) for tasks I am planning on working on. Check them out if you wish to contribute in that way.

**Want to add features?** Before you decide to take a bunch of time and add functionality to the library, please, [create an issue]
(https://github.com/levibostian/dotenv-android/issues/new) stating what you wish to add. This might save you some time in case your purpose does not fit well in the use cases of this project.
