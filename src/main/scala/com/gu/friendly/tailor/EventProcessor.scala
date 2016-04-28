package com.gu.friendly.tailor

import java.util.{List => JList}

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.{AttributeAction, AttributeValue, AttributeValueUpdate}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.model.Record
import com.typesafe.scalalogging.LazyLogging
import ophan.thrift.event.Event

import scala.collection.convert.wrapAll._

class EventProcessor() extends IRecordProcessor with LazyLogging {

  import EventProcessor._

  private[this] var shardId: String = _

  override def initialize(shardId: String): Unit = {
    this.shardId = shardId
    logger.info(s"Initialized an event processor for shard $shardId")
  }

  override def processRecords(records: JList[Record], checkpointer: IRecordProcessorCheckpointer): Unit = {
    val actions = records
      .map(deserializeToEvent).filter(ev => ev.pageView.exists(pv => isInteresting(pv.page.url.path)))

    print(s"${actions.size} ")
    actions.foreach(processPageView)

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

  val dynamoDBClient:AmazonDynamoDBClient  = new AmazonDynamoDBClient(new ProfileCredentialsProvider("membership")).withRegion(EU_WEST_1)

  val isInteresting = Set(
    "/commentisfree/2016/apr/28/david-cameron-unions-brexit-trade-union-bill-brendan-barber",
    "/politics/2016/apr/28/leave-campaign-economists-for-brexit-report"
  )

  def deserializeToEvent(record: Record): Event =
    ThriftSerializer.fromByteBuffer(record.getData)(Event.decoder)

    def processPageView(ev: Event) = for {
      pv <- ev.pageView
      path = pv.page.url.path if isInteresting(path)
    } {

      val keyMap = Map(
        "browserId" -> new AttributeValue().withS(ev.browserId.id),
        "userId" -> new AttributeValue().withS(ev.userId.getOrElse("None"))
      )

      dynamoDBClient.updateItem(
        "roberto-table-test",
        keyMap,
        Map("politics/eu-referendum" -> new AttributeValueUpdate().withAction(AttributeAction.ADD).withValue(new AttributeValue().withSS(path)))
      )
    }
}

object EventProcessorFactory extends IRecordProcessorFactory {
  override def createProcessor(): EventProcessor = new EventProcessor()
}