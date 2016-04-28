package com.gu.friendly.tailor

object Main extends App {

  EventsConsumer.start()
  println("Sleeping")
  Thread.sleep(20 * 1000L)
  println("Going to stop")
  EventsConsumer.stop()
  println("Should be all done now!")
}
