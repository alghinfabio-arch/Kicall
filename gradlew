#!/bin/sh
exec java -classpath "${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-8.4-bin/*/gradle-8.4/lib/gradle-launcher-8.4.jar" org.gradle.launcher.GradleMain "$@"
