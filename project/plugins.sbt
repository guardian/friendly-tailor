addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1") // https://github.com/sbt/sbt-buildinfo/issues/88

addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "4.7.0") // https://dl.bintray.com/sbt/sbt-plugin-releases/com.twitter/scrooge-sbt-plugin/scala_2.10/sbt_0.13/

//the twitter repository
resolvers += "twitter-repo" at "https://maven.twttr.com"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.6")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.8.4")

addSbtPlugin("com.localytics" % "sbt-dynamodb" % "1.4.1")
