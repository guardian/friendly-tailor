package com.gu.friendly.tailor

import java.time.Instant
import java.util.{List => JList}

import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.model.Record
import com.typesafe.scalalogging.LazyLogging
import ophan.thrift.event.{AssignedId, Event}

import scala.collection.convert.wrapAll._
import scala.util.Try

case class RelevantPageView(
  path: String,
  tags: Set[String],
  time: Instant,
  browserId: AssignedId,
  userId: Option[String]
)

class EventProcessor() extends IRecordProcessor with LazyLogging {

  import EventProcessor._

  private[this] var shardId: String = _

  override def initialize(shardId: String): Unit = {
    this.shardId = shardId
    logger.info(s"Initialized an event processor for shard $shardId")
  }

  override def processRecords(records: JList[Record], checkpointer: IRecordProcessorCheckpointer): Unit = {
    val allEvents = records.map(deserializeToEvent)
    val relevantPageViews = allEvents.flatMap(relevantPageView)
    val earliestEvent = allEvents.minBy(_.dt).instant
    val latestEvent = allEvents.maxBy(_.dt).instant
    logger.info(s"received ${allEvents.size} events, ${relevantPageViews.size} relevant page views, earliest event at $earliestEvent, most recent event at $latestEvent")
    relevantPageViews.foreach { pageView =>
      Try(pageViewStorage.putPageView(pageView)).recover {
        case e => logger.error(s"failed to process $pageView", e)
      }
    }

    checkpointer.checkpoint()
  }

  // This method may be called by KCL, e.g. in case of shard splits/merges
  override def shutdown(checkpointer: IRecordProcessorCheckpointer, reason: ShutdownReason): Unit = {
    if (reason == ShutdownReason.TERMINATE) {
      checkpointer.checkpoint()
    }
    logger.info(s"Shutdown event processor for shard $shardId because $reason")
  }
}

object EventProcessor extends LazyLogging {

  val dynamoDBClient:AmazonDynamoDBClient  = new AmazonDynamoDBClient(EventsConsumer.defaultCredentialsProvider).withRegion(EU_WEST_1)

  val tableName = s"${Config.app}-${Config.stage}"

  val pageViewStorage = new PageViewStorage(dynamoDBClient)

  logger.info(s"Table name = $tableName")

  implicit class RichEvent(event: Event) {
    lazy val instant = Instant.ofEpochMilli(event.dt)
  }

  def deserializeToEvent(record: Record): Event =
    ThriftSerializer.fromByteBuffer(record.getData)(Event.decoder)

  def relevantPageView(ev: Event):Option[RelevantPageView] = for {
    pv <- ev.pageView
    path = pv.page.url.path
    tagsForPath = MonitoredTags.tagsForPath(path)
    if tagsForPath.nonEmpty
  } yield RelevantPageView(path, tagsForPath, ev.instant, ev.browserId, ev.userId)

}

object EventProcessorFactory extends IRecordProcessorFactory {
  override def createProcessor(): EventProcessor = new EventProcessor()
}