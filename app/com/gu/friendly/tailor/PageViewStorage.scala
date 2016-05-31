package com.gu.friendly.tailor

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.ReturnValue._
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, UpdateItemRequest, UpdateItemResult}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.convert.decorateAsScala._
import scala.collection.convert.wrapAll._

class PageViewStorage(client: AmazonDynamoDBClient) extends LazyLogging {

  val table = EventProcessor.tableName

  def entryKeyFor(relevantPageView: RelevantPageView): Map[String, AttributeValue] =
    Map("browserId" -> new AttributeValue().withS(relevantPageView.browserId.id), "userId" -> new AttributeValue().withS(relevantPageView.userId.getOrElse("None")))

  def putPageView(relevantPageView: RelevantPageView) {
    val entryKey = entryKeyFor(relevantPageView)
    val timeAttribute = new AttributeValue().withN(relevantPageView.time.toEpochMilli.toString)
    val pathTimeAttribute = new AttributeValue().addMEntry(relevantPageView.path, timeAttribute)

    def executeUpdate(configure: UpdateItemRequest => UpdateItemRequest): UpdateItemResult = {
      client.updateItem(configure(new UpdateItemRequest().withTableName(table).withKey(entryKey)))
    }

    val setUserIdString= relevantPageView.userId.map(_ => s", userIdIndexKey = :userId").mkString
    val setUserExpressionValue: Map[String, AttributeValue] =
      relevantPageView.userId.map(userId => ":userId" -> new AttributeValue().withS(userId)).toMap

      relevantPageView.tags.foreach { tag =>
      def hasSetPageView(updateResult: UpdateItemResult): Boolean = (for {
        attributes <- Option(updateResult.getAttributes)
        pageViewsByTag <- attributes.asScala.get("pageViewsByTag")
        timeByPathAttribute <- pageViewsByTag.getM.asScala.get(tag)
        timeAttributeInDb <- timeByPathAttribute.getM.asScala.get(relevantPageView.path)
      } yield timeAttributeInDb == timeAttribute).getOrElse(false)

      val initialMapResult = executeUpdate(_.withUpdateExpression("SET pageViewsByTag = if_not_exists(pageViewsByTag,:initialMapResult)"+setUserIdString)
        .withReturnValues(UPDATED_NEW)
        .withExpressionAttributeValues(setUserExpressionValue ++ Map(":initialMapResult" -> new AttributeValue().addMEntry(tag, pathTimeAttribute))))

      if (!hasSetPageView(initialMapResult)) {
        val tagMapResult = executeUpdate(_.withUpdateExpression("SET pageViewsByTag.#tag = if_not_exists(pageViewsByTag.#tag, :initialTagMap)"+setUserIdString)
          .withReturnValues(UPDATED_NEW)
          .withExpressionAttributeNames(Map("#tag" -> tag))
          .withExpressionAttributeValues(setUserExpressionValue ++ Map(":initialTagMap" -> pathTimeAttribute)))

        if (!hasSetPageView(tagMapResult)) {
          executeUpdate(_.withUpdateExpression("SET pageViewsByTag.#tag.#path = :time"+setUserIdString)
            .withExpressionAttributeNames(Map("#tag" -> tag, "#path" -> relevantPageView.path))
            .withExpressionAttributeValues(setUserExpressionValue ++ Map(":time" -> timeAttribute)))
        }
      }
    }
  }
}
