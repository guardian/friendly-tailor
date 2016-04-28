name := "friendly-tailor"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(
    BuildInfoPlugin
).settings(
    buildInfoKeys := Seq[BuildInfoKey](
	name,
	BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse (try {
	    "git rev-parse HEAD".!!.trim
	} catch {
	    case e: Exception => "unknown"
	})),
	BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
	BuildInfoKey.constant("buildTime", System.currentTimeMillis)
    ),
    buildInfoPackage := "app",
    buildInfoOptions += BuildInfoOption.ToMap
    )

scalaVersion := "2.11.8"
scalacOptions ++= Seq("-feature")


libraryDependencies ++= Seq(
    "com.amazonaws" % "amazon-kinesis-client" % "1.6.2"
)

testOptions in Test ++= Seq(
    Tests.Argument("-oD") // display full stack errors and execution times in Scalatest output
)

