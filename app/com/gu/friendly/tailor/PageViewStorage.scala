package com.gu.friendly.tailor

import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsyncClient, AmazonDynamoDBClient}
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, UpdateItemRequest}

import scala.collection.convert.wrapAsJava._

class PageViewStorage(client: AmazonDynamoDBClient) {

  val table = EventProcessor.tableName

  def entryKeyFor(relevantPageView: RelevantPageView): Map[String, AttributeValue] =
    Map("browserId" -> new AttributeValue().withS(relevantPageView.browserId.id), "userId" -> new AttributeValue().withS(relevantPageView.userId.getOrElse("None")))


  def putPageView(relevantPageView: RelevantPageView) {

    val entryKey = entryKeyFor(relevantPageView)
    val timeAttribute = new AttributeValue().withN(relevantPageView.time.toEpochMilli.toString)
    val pathTimeAttribute = new AttributeValue().addMEntry(relevantPageView.path, timeAttribute)
    relevantPageView.tags.foreach { tag =>

      client.updateItem(new UpdateItemRequest()
        .withTableName(table)
        .withKey(entryKey)
        .withUpdateExpression("SET pageViewsByTag = if_not_exists(pageViewsByTag,:initialMap)")
        .withExpressionAttributeValues(Map(":initialMap" -> new AttributeValue().addMEntry(tag, pathTimeAttribute))))

      client.updateItem(new UpdateItemRequest()
        .withTableName(table)
        .withKey(entryKey)
        .withUpdateExpression("SET pageViewsByTag.#tag = if_not_exists(pageViewsByTag.#tag, :initialTagMap)")
        .withExpressionAttributeNames(Map("#tag" -> tag))
        .withExpressionAttributeValues(Map(":initialTagMap" -> pathTimeAttribute)))

      client.updateItem(new UpdateItemRequest()
        .withTableName(table)
        .withKey(entryKey)
        .withUpdateExpression("SET pageViewsByTag.#tag.#path = :time")
        .withExpressionAttributeNames(Map("#tag" -> tag, "#path" -> relevantPageView.path))
        .withExpressionAttributeValues(Map(":time" -> timeAttribute)))
    }
  }
}
