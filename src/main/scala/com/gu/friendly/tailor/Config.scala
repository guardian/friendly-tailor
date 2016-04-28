package com.gu.friendly.tailor

import com.typesafe.config.ConfigFactory

object Config {
  val conf = ConfigFactory.load()

  val stack = conf.getString("stack")
  val app = conf.getString("app")
  val stage = conf.getString("stage")

  val indexNamePrefix = "uhapi"
  val index = conf.getBoolean("index")


}