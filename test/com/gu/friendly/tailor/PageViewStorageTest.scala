package com.gu.friendly.tailor

import java.util

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest, UpdateItemRequest}

import scala.collection.convert.wrapAsJava._
import scala.collection.convert.decorateAll._

class PageViewStorageTest extends org.scalatest.FunSpec with org.scalatest.Matchers {
  it("should update a nested map") {
    val client = LocalDynamoDB.client()

    val table = "fish"
    val entryKey = Map("browserId" -> new AttributeValue().withS("foo"), "userId" -> new AttributeValue().withS("bar"))

    def putPageView(tag: String, path: String, time: Long) {
      client.updateItem(new UpdateItemRequest()
        .withTableName(table)
        .withKey(entryKey)
        .withUpdateExpression("SET pageViewsByTag = if_not_exists(pageViewsByTag,:initialMap)")
        .withExpressionAttributeValues(Map(":initialMap" -> new AttributeValue().addMEntry(tag, new AttributeValue().addMEntry(path, new AttributeValue().withN(time.toString))))))

      client.updateItem(new UpdateItemRequest()
          .withTableName(table)
          .withKey(entryKey)
          .withUpdateExpression("SET pageViewsByTag.#tag = if_not_exists(pageViewsByTag.#tag, :initialTagMap)")
          .withExpressionAttributeNames(Map("#tag" -> tag))
          .withExpressionAttributeValues(Map(":initialTagMap" -> new AttributeValue().addMEntry(path, new AttributeValue().withN(time.toString))))
      )

      client.updateItem(new UpdateItemRequest()
        .withTableName(table)
        .withKey(entryKey)
        .withUpdateExpression("SET pageViewsByTag.#tag.#path = :time")
        .withExpressionAttributeNames(Map("#tag" -> tag, "#path" -> path))
        .withExpressionAttributeValues(Map(":time" -> new AttributeValue().withN(time.toString))))
    }

    LocalDynamoDB.usingTable(client)(table)('browserId -> S, 'userId -> S) {


      putPageView("politics/eu-referendum", "/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum", 1234)

      client.getItem(table, entryKey).getItem().get("pageViewsByTag").getM should contain key "politics/eu-referendum"

      putPageView("profile/roberto-tyley", "/info/developer-blog/2015/feb/03/prout-is-your-pull-request-out", 5678)

      client.getItem(table, entryKey).getItem().get("pageViewsByTag").getM.keySet().asScala should be (Set("politics/eu-referendum","profile/roberto-tyley"))
    }
  }
}