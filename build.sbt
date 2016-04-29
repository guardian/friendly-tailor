name := "friendly-tailor"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

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

testOptions in Test ++= Seq(
    Tests.Argument("-oD") // display full stack errors and execution times in Scalatest output
)

