package controllers

import com.typesafe.scalalogging.LazyLogging
import play.api.mvc._

object Management extends Controller with LazyLogging {

  def healthcheck = Action {
    Ok("OK")
  }

}
