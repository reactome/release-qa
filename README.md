# ReleaseQA
A collection of tools to perform QA checks during the Release process.

Use the build\_all.sh script to build all of the projects into a single JAR. This will also install QACommon into your local repository before it builds the other projects.

## QACommon
This project is a library containing common code that is used by the various QA checks. This is not a child module of release-qa. This must be built and installed into your local repo before you can build the other projects.

## release-qa
Parent project

## release-qa-distribution
This is a "distribution" module. Building the release-qa project (with the "package" goal) will build a single jar in release-qa-distribution that contains ALL of the other QA checks, and their dependencies.


