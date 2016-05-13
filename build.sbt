name := "friendly-tailor"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(
  BuildInfoPlugin,
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
  "com.gu" %% "content-api-scala-client" % "8.2.1",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false


scroogeThriftOutputFolder in Compile := sourceManaged.value / "thrift"

managedSourceDirectories in Compile += (scroogeThriftOutputFolder in Compile).value

testOptions in Test ++= Seq(
    Tests.Argument("-oD") // display full stack errors and execution times in Scalatest output
)

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd

debianPackageDependencies := Seq("openjdk-8-jre-headless")

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null",
  "-J-XX:MaxRAMFraction=2",
  "-J-XX:InitialRAMFraction=2",
  "-J-XX:MaxMetaspaceSize=500m",
  "-J-XX:+PrintGCDetails",
  "-J-XX:+PrintGCDateStamps",
  s"-J-Xloggc:/var/log/${name.value}/gc.log"
)

maintainer := "Membership Discovery <membership.dev@theguardian.com>"

packageSummary := "Friendly Tailor service"

packageDescription := """Friendly Tailor reads and stores Ophan events to measure reader background knowledge"""

riffRaffPackageType := (packageBin in Debian).value

def env(key: String): Option[String] = Option(System.getenv(key))

riffRaffUploadArtifactBucket := Option("riffraff-artifact")

riffRaffUploadManifestBucket := Option("riffraff-builds")

riffRaffManifestBranch := env("BRANCH_NAME").getOrElse("unknown_branch")

riffRaffBuildIdentifier := env("BUILD_NUMBER").getOrElse("DEV")

riffRaffManifestVcsUrl  := "git@github.com:guardian/friendly-tailor.git"


buildInfoKeys := Seq[BuildInfoKey](
  name,
  BuildInfoKey.constant("gitCommitId", env("BUILD_VCS_NUMBER") getOrElse (try {
    "git rev-parse HEAD".!!.trim
  } catch {
    case e: Exception => "unknown"
  })),
  BuildInfoKey.constant("buildNumber", env("BUILD_NUMBER") getOrElse "DEV"),
  BuildInfoKey.constant("buildTime", System.currentTimeMillis)
)

buildInfoPackage := "app"

startDynamoDBLocal <<= startDynamoDBLocal.dependsOn(compile in Test)
test in Test <<= (test in Test).dependsOn(startDynamoDBLocal)
testOptions in Test <+= dynamoDBLocalTestCleanup