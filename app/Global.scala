import com.gu.friendly.tailor.EventsConsumer
import play.api.{Application, GlobalSettings}

object Global extends GlobalSettings  {
  override def onStart(app: Application) {
    EventsConsumer.start()
  }

  override def onStop(app: Application) {
    EventsConsumer.stop()
  }
}
