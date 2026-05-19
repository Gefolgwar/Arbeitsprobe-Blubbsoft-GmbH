#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF)
# Maven Wrapper script for UN*X
# ----------------------------------------------------------------------------

set -e

if [ -z "$MAVEN_HOME" ]; then
  MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.9"
fi

WRAPPER_JAR="$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$(dirname "$0")/.mvn/wrapper/maven-wrapper.properties"
WRAPPER_URL=$(grep 'wrapperUrl' "$WRAPPER_PROPERTIES" | cut -d'=' -f2-)
DIST_URL=$(grep 'distributionUrl' "$WRAPPER_PROPERTIES" | cut -d'=' -f2-)

MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" && pwd)"
export MAVEN_PROJECTBASEDIR

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Downloading Maven Wrapper..."
  if command -v curl > /dev/null 2>&1; then
    curl -sL -o "$WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "Error: curl or wget is required to download the Maven Wrapper."
    exit 1
  fi
fi

MAVEN_DIR="$HOME/.m2/wrapper/dists/apache-maven-3.9.9"
if [ ! -d "$MAVEN_DIR" ]; then
  echo "Downloading Maven distribution..."
  TEMP_FILE=$(mktemp)
  if command -v curl > /dev/null 2>&1; then
    curl -sL -o "$TEMP_FILE" "$DIST_URL"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$TEMP_FILE" "$DIST_URL"
  fi
  mkdir -p "$HOME/.m2/wrapper/dists"
  unzip -q -o "$TEMP_FILE" -d "$HOME/.m2/wrapper/dists"
  rm -f "$TEMP_FILE"
fi

exec java $MAVEN_OPTS \
  -classpath "$WRAPPER_JAR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
