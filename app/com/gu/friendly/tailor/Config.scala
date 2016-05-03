package com.gu.friendly.tailor

import com.typesafe.config.ConfigFactory

object Config {
  val conf = ConfigFactory.load()

  val stack = conf.getString("stack")
  val app = conf.getString("app")
  val stage = conf.getString("stage")
  val contentApiKey = conf.getString("content.api.key")
  val contentTargetUrl = conf.getString("content.target.url")
}