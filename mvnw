#!/bin/sh
# Maven Wrapper script
# Uses maven-wrapper.jar to download and run Maven

BASEDIR=$(cd "$(dirname "$0")" && pwd)
WRAPPER_JAR="$BASEDIR/.mvn/wrapper/maven-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Error: maven-wrapper.jar not found at $WRAPPER_JAR"
    echo "Please ensure the .mvn/wrapper directory exists with maven-wrapper.jar"
    exit 1
fi

# Find Java executable
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if [ ! -x "$JAVACMD" ] && ! command -v "$JAVACMD" > /dev/null 2>&1; then
    echo "Error: JAVA_HOME is not set and 'java' is not on PATH"
    exit 1
fi

export MAVEN_PROJECTBASEDIR="$BASEDIR"

exec "$JAVACMD" $MAVEN_OPTS \
    -Dmaven.multiModuleProjectDirectory="$BASEDIR" \
    -classpath "$WRAPPER_JAR" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"
