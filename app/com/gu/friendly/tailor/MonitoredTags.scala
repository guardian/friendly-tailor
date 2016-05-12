package com.gu.friendly.tailor

import java.time.Instant

import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.SearchQuery
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Akka

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Play.current

object MonitoredTags extends LazyLogging {

  case class TagFetchData(fetchTime: Instant, paths: Set[String])

  val client = new GuardianContentClient(Config.contentApiKey){
    override val targetUrl=Config.contentTargetUrl
  }

  val tags = Set("politics/eu-referendum", "profile/roberto-tyley")

  val interestingContent = TrieMap.empty[String, TagFetchData]

  def start() = {
    logger.info("Starting")
    Akka.system.scheduler.schedule(1.second, 1.minute){
      updateInterestingContent()
    }
  }

  def oldestTagFetchData(): Option[Instant] =
    interestingContent.values.map(_.fetchTime).toSeq.sorted.headOption

  def tagsForPath(path: String): Set[String] = tags.filter(tag => interestingContent.get(tag).exists(_.paths.contains(path)))

  def updateInterestingContent() = Future.sequence(for { tag <- tags } yield {
    val f = for {
      result <- client.getResponse(SearchQuery().tag(tag).pageSize(200))
    } yield {
      val pathSet = result.results.map(c => s"/${c.id}").toSet
      logger.info(s"$tag : ${pathSet.size}")
      interestingContent(tag) = TagFetchData(Instant.now(), pathSet)
    }
    f.onFailure{
      case e => logger.error(s"problem getting $tag", e)
    }
    f
  })
}
