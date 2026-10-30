# FinNUS

FinNUS is a command-line finance tracker for university students on a tight monthly budget, including international
students who receive allowances in foreign currencies. Users record deposits and withdrawals, tag spending with
categories, track their monthly allowance cycle and see how much money they have left, all by typing short commands.

* [User Guide](https://ay2627s1-cs2113-w13-2.github.io/tp/UserGuide.html)
* [Developer Guide](https://ay2627s1-cs2113-w13-2.github.io/tp/DeveloperGuide.html)
* [About Us](https://ay2627s1-cs2113-w13-2.github.io/tp/AboutUs.html)

## Setting up in IntelliJ

Prerequisites: JDK 25 (use the exact version), and the most recent version of IntelliJ.

1. **Ensure IntelliJ JDK 25 is defined as an SDK**, as described [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk).
   This step is not needed if you have used JDK 25 in a previous IntelliJ project.
1. **Import the project _as a Gradle project_**, as described [here](https://se-education.org/guides/tutorials/intellijImportGradleProject.html).
1. **Verify the setup**: after the import is complete, locate `src/main/java/finnus/FinNUS.java`, right-click it and
   choose `Run 'FinNUS.main()'`. On the first run, FinNUS starts its first-time setup:
   ```
   ____________________________________________________________
   First-time setup
   Enter your current opening balance in SGD.
   This is your starting balance and will not be recorded as a transaction.
   ____________________________________________________________
   Opening balance (SGD):
   ```
   Enter an amount (e.g. `100`), confirm with `yes`, and type `help` to see all commands.
1. **Enable assertions** for the run configuration: `Run` > `Edit Configurations...` > `FinNUS` > `Modify options` >
   `Add VM options`, and enter `-ea`.

FinNUS saves its data in `finnus.txt` (with a backup in `finnus.txt.bak`) and writes logs to `finnus.log`, all in
the folder it is run from. These files are ignored by Git.

**Warning:** Keep the `src/main/java` folder as the root folder for Java files (i.e., don't rename those folders or
move Java files to another folder outside of this folder path), as this is the default location some tools (e.g.,
Gradle) expect to find Java files.

## Build automation using Gradle

This project uses Gradle for build automation and dependency management. Common tasks:

* `./gradlew run`: runs FinNUS, with assertions enabled
* `./gradlew test`: runs the JUnit tests
* `./gradlew check`: runs the tests and Checkstyle
* `./gradlew shadowJar`: builds the executable `build/libs/finnus.jar`, which can be run with `java -jar finnus.jar`

On Windows, use `gradlew.bat` instead of `./gradlew`. If you are new to Gradle, refer to the
[Gradle Tutorial at se-education.org/guides](https://se-education.org/guides/tutorials/gradle.html).

## Testing

JUnit tests are in `src/test/java/finnus`, in the same packages as the classes they test. Run them with
`./gradlew test`, or right-click the `src/test/java` folder in IntelliJ and choose `Run 'All Tests'`.

If you are new to JUnit, refer to the [JUnit Tutorial at se-education.org/guides](https://se-education.org/guides/tutorials/junit.html).

## Checkstyle

The Checkstyle rules are in `config/checkstyle/checkstyle.xml`, and are checked by `./gradlew check`.
If you are new to Checkstyle, refer to the [Checkstyle Tutorial at se-education.org/guides](https://se-education.org/guides/tutorials/checkstyle.html).

## CI using GitHub Actions

The project uses [GitHub Actions](https://github.com/features/actions) for CI. When you push a commit to this repo or
open a PR against it, GitHub Actions builds the project and runs `./gradlew check` on Windows, macOS and Linux.

## Documentation

The `/docs` folder contains the project documentation, published with GitHub Pages from the `master` branch's
`/docs` folder:

* `docs/UserGuide.md`: how to use FinNUS
* `docs/DeveloperGuide.md`: design and implementation, with UML diagrams (PlantUML sources are in `docs/diagrams`
  and the exported images in `docs/images`)
* `docs/AboutUs.md` and `docs/team/`: the team and each member's project portfolio page
