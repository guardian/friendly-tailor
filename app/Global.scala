import com.gu.friendly.tailor.EventsConsumer
import com.typesafe.scalalogging.LazyLogging
import play.api.{Application, GlobalSettings}

object Global extends GlobalSettings with LazyLogging {
  override def onStart(app: Application) {
    logger.info("starting")
    EventsConsumer.start()
  }

  override def onStop(app: Application) {
    EventsConsumer.stop()
  }
}
