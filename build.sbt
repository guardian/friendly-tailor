name := "friendly-tailor"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(
  PlayScala,
  RiffRaffArtifact,
  JDebPackaging
)

scalaVersion := "2.11.8"
scalacOptions ++= Seq("-feature")


libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "com.typesafe" % "config" % "1.3.0",
  "org.apache.thrift" % "libthrift" % "0.9.3",
  "com.twitter" %% "scrooge-core" % "4.7.0",
  "com.amazonaws" % "amazon-kinesis-client" % "1.6.2",
  "com.amazonaws" % "aws-java-sdk-sts" % "1.10.73",
  "com.gu" %% "content-api-scala-client" % "8.2.1"
)

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

testOptions in Test ++= Seq(
    Tests.Argument("-oD") // display full stack errors and execution times in Scalatest output
)

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd

debianPackageDependencies := Seq("openjdk-8-jre-headless")

maintainer := "The Maintainer <the.maintainer@company.com>"

packageSummary := "Brief description"

packageDescription := """Slightly longer description"""


riffRaffPackageType := (packageBin in Debian).value

def env(key: String): Option[String] = Option(System.getenv(key))

riffRaffBuildIdentifier := env("TRAVIS_BUILD_NUMBER").getOrElse("DEV")

riffRaffUploadArtifactBucket := Option("riffraff-artifact")

riffRaffUploadManifestBucket := Option("riffraff-builds")