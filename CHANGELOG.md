# [0.2.1] - 2020-03-26

## Fixed
- If env var not found in .env file, do not add it to the generated Env file.

# [0.2.0] - 2020-03-26

## Changed
- Allow package name to exist in the .env file instead of CLI param
- Show warning message instead of abort when a value is not found in .env file. 

# [0.1.2] - 2020-03-25

## Fixed 
- Special characters at end of var ignored 

# [0.1.1] - 2019-11-30

## Fixed 
- Tested out CLI on a local Android Studio project and found bugs with it. 
- Fix README docs on how to create Gradle build task. 

# [0.1.0] - 2019-11-30

## Added
- Create CLI that reads `.env` files and generates `Env.kt` file with values requested in source code.