package controllers

import com.gu.friendly.tailor.EventsConsumer
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc._

object Management extends Controller with LazyLogging {

  def healthcheck = Action {
    Ok(s"OK\n${app.BuildInfo.gitVersionId}\n${EventsConsumer.worker}")
  }

}
