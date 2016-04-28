package com.gu.friendly.tailor

import java.util.{List => JList}

import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.model.Record
import com.typesafe.scalalogging.LazyLogging
import ophan.thrift.event.Event

import scala.collection.convert.wrapAsScala._

class EventProcessor() extends IRecordProcessor with LazyLogging {

  import EventProcessor._

  private[this] var shardId: String = _

  override def initialize(shardId: String): Unit = {
    this.shardId = shardId
    logger.info(s"Initialized an event processor for shard $shardId")
  }

  override def processRecords(records: JList[Record], checkpointer: IRecordProcessorCheckpointer): Unit = {
    val actions = records
      .map(deserializeToEvent)

    print(s"${actions.size} ")

//      .filter(isValid)
//      .flatMap(toViewEvent)
      // .foreach(saveViewEvent(es, _))

    // Don't forget to tell Kinesis that we've processed the records
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
  private[this] val blacklistedUrls = Set("profile.theguardian.com")

  def deserializeToEvent(record: Record): Event =
    ThriftSerializer.fromByteBuffer(record.getData)(Event.decoder)

//
//  def toViewEvent(ev: Event): Option[Action] = {
//    ev.pageView map { pv =>
//      val browserId = ev.browserId.id
//      Action(
//        browserId = browserId,
//        platform = Device(pv),
//        userId = ev.userId,
//        url = pv.page.url.raw,
//        section = pv.page.section,
//        timestamp = new DateTime(ev.dt, DateTimeZone.UTC),
//        pageType = PageType(pv))
//    }
//  }
//
//  def isValid(ev: Event): Boolean = ev.pageView.exists { pv =>
//    val isBlacklisted = blacklistedUrls.exists(blUrl => pv.page.url.raw.contains(blUrl))
//    val isValid = pv.validity == SuspectStatus.Valid || pv.validity == SuspectStatus.InvalidInternalGuardianTraffic
//    isValid && !isBlacklisted
//  }
}

object EventProcessorFactory extends IRecordProcessorFactory {
  override def createProcessor(): EventProcessor = {
    new EventProcessor()
  }
}