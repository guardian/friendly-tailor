package com.gu.friendly.tailor

import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.SearchQuery
import com.typesafe.scalalogging.LazyLogging

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MonitoredTags extends LazyLogging {

  val client = new GuardianContentClient(Config.contentApiKey){
    override val targetUrl=Config.contentTargetUrl
  }

  val tags = Set("politics/eu-referendum", "profile/roberto-tyley")

  val interestingContent = TrieMap.empty[String, Set[String]]



  def tagsForPath(path: String): Set[String] = tags.filter(tag => interestingContent.get(tag).exists(_.contains(path)))

  def updateInterestingContent() = Future.sequence(for { tag <- tags } yield {
    val f = for {
      result <- client.getResponse(SearchQuery().tag(tag).pageSize(200))
    } yield {
      val pathSet = result.results.map(c => s"/${c.id}").toSet
      logger.info(s"$tag : ${pathSet.size}")
      interestingContent(tag) = pathSet
    }
    f.onFailure{
      case e => logger.error(s"problem getting $tag", e)
    }
    f
  })
}
