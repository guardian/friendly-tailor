package controllers

import java.time.Instant
import java.time.temporal.ChronoUnit.MINUTES

import com.gu.friendly.tailor.{EventsConsumer, MonitoredTags}
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc._

object Management extends Controller with LazyLogging {

  def healthcheck = Action {
    val oldestTagFetchData = MonitoredTags.oldestTagFetchData()
    val message = s"${app.BuildInfo.gitCommitId}\n${EventsConsumer.worker}\noldestTagFetchData: ${oldestTagFetchData.mkString}"
    if (oldestTagFetchData.exists(_.isAfter(Instant.now.minus(10, MINUTES)))) Ok(message) else ServiceUnavailable(message)
  }

}
