package com.gu.friendly.tailor

import java.util

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest, UpdateItemRequest}

import scala.collection.convert.wrapAsJava._

class PageViewStorageTest extends org.scalatest.FunSpec with org.scalatest.Matchers {
  it("should update a nested map") {
    val client = LocalDynamoDB.client()

    val table = "fish"
    val entryKey = Map("browserId" -> new AttributeValue().withS("foo"), "userId" -> new AttributeValue().withS("bar"))

    def putPageView(tag: String, path: String, time: Long) {
      val putItemRequest = new PutItemRequest()
        .withTableName(table)
        .withItem(entryKey ++ Map("pageViewsByTag" -> new AttributeValue().withM(Map.empty[String, AttributeValue])))

      client.putItem(putItemRequest)

      val updateItemRequest = new UpdateItemRequest()
        .withTableName(table)
        .withKey(entryKey)
        .withUpdateExpression("SET pageViewsByTag.#tag = :time")
        .withExpressionAttributeNames(Map("#tag" -> "politics/eu-referendum"))
        .withExpressionAttributeValues(Map(":time" -> new AttributeValue().withN(time.toString)))

      client.updateItem(updateItemRequest)
    }

    LocalDynamoDB.usingTable(client)(table)('browserId -> S, 'userId -> S) {


      putPageView("politics/eu-referendum", "/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum", 1234)

      client.getItem(table, entryKey).getItem().get("pageViewsByTag").getM.size() should be(1)

      putPageView("profile/roberto-tyley", "/info/developer-blog/2015/feb/03/prout-is-your-pull-request-out", 5678)

      client.getItem(table, entryKey).getItem().get("pageViewsByTag").getM.size() should be(2)
    }
  }
}