#!/bin/sh

##############################################################################
# Gradle start up script for POSIX
##############################################################################

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

APP_HOME=$(cd "$(dirname "$0")" && pwd)

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

JAVACMD="java"

exec "$JAVACMD" "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
