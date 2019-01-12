// General settings.
organization  := "org.toktok"
name          := "tox4j-c"
version       := "0.2.0"
scalaVersion  := "2.11.12"

bintrayVcsUrl := Some("https://github.com/TokTok/jvm-toxcore-c")

/******************************************************************************
 * Dependencies
 ******************************************************************************/

// Snapshot and linter repository.
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.bintrayRepo("toktok", "maven")

// Build dependencies.
libraryDependencies ++= Seq(
  "org.toktok" %% "tox4j-api" % version.value,
  "org.toktok" %% "macros" % "0.1.1",
  "com.chuusai" %% "shapeless" % "2.3.3",
  "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % "0.5.43",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
)

// Test dependencies.
libraryDependencies ++= Seq(
  "com.storm-enroute" %% "scalameter" % "0.7",
  "jline" % "jline" % "2.14.2",
  "junit" % "junit" % "4.12",
  "org.scalacheck" %% "scalacheck" % "1.13.4",
  "org.scalatest" %% "scalatest" % "3.0.1",
  "org.scalaz" %% "scalaz-concurrent" % "7.2.8",
  "org.slf4j" % "slf4j-log4j12" % "1.7.22"
) map (_ % Test)

// Add ScalaMeter as test framework.
testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

// Disable parallel test execution, as network tests become flaky that way.
parallelExecution in Test := false

/******************************************************************************
 * Other settings and plugin configuration.
 ******************************************************************************/

// TODO(iphydf): Require less test coverage for now, until ToxAv is tested.
import scoverage.ScoverageKeys._
coverageMinimum := 20
coverageExcludedPackages := ".*\\.proto\\..*"

import im.tox.sbt.Scalastyle
Scalastyle.projectSettings

// Mixed project.
compileOrder := CompileOrder.Mixed
scalaSource in Compile := (javaSource in Compile).value
scalaSource in Test    := (javaSource in Test   ).value

// Override Scalastyle configuration for test.
scalastyleConfigUrl in Test := None
scalastyleConfig in Test := (scalaSource in Test).value / "scalastyle-config.xml"

unmanagedSourceDirectories in Compile += baseDirectory.value / "src/main/resources"
lazy val execBuild = taskKey[Unit]("local build")
// This prepends the String you would type into the shell
lazy val startupTransition: State => State = { s: State =>
  "execBuild" :: s
}
lazy val root = (project in file("."))
  .settings(
    execBuild := { "./build.sh" ! },
      onLoad in Global := {
      val old = (onLoad in Global).value
      // compose the new transition on top of the existing one
      // in case your plugins are using this hook.
      startupTransition compose old
    }
  )
//compile := (Compile / compile dependsOn execBuild).value
//publishM2 <<=(publishM2) dependsOn execBuild