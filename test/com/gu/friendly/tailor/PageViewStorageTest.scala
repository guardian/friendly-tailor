package com.gu.friendly.tailor

import java.util

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest, UpdateItemRequest}
import ophan.thrift.event.AssignedId

import scala.collection.convert.wrapAsJava._
import scala.collection.convert.decorateAll._
import com.gu.scanamo._
import com.gu.scanamo.syntax._

case class StoredPageView(browserId: String, userId: String, pageViewsByTag: Map[String, Map[String, Long]])

class PageViewStorageTest extends org.scalatest.FunSpec with org.scalatest.Matchers {

  val client = LocalDynamoDB.client()
  val pageViewStorage = new PageViewStorage(client)
  val browserFoo = AssignedId("foo")
  val fooLooksAtJez = RelevantPageView(
    "/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum",
    Set("politics/eu-referendum"),
    browserFoo,
    None
  )
  val fooLooksAtRoberto = RelevantPageView(
    "/info/developer-blog/2015/feb/03/prout-is-your-pull-request-out",
    Set("profile/roberto-tyley"),
    browserFoo,
    None
  )
  val fooLooksAtBojo = RelevantPageView(
    "/politics/2016/may/17/boris-johnon-no-guarantee-vote-to-remain-will-settle-eu-issue-for-ever",
    Set("politics/eu-referendum"),
    browserFoo,
    None
  )
  val entryKey = pageViewStorage.entryKeyFor(fooLooksAtJez)
  val table = pageViewStorage.table

  def getFoo(): StoredPageView = {
    Scanamo.get[StoredPageView](client)(table)('browserId -> "foo" and 'userId -> "None").get.toOption.get
  }

  it("should update a nested map") {

    LocalDynamoDB.usingTable(client)(table)('browserId -> S, 'userId -> S) {

      pageViewStorage.putPageView(fooLooksAtJez, 1234)

      getFoo().pageViewsByTag should equal(Map("politics/eu-referendum" -> Map("/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum" -> 1234)))

      pageViewStorage.putPageView(fooLooksAtRoberto, 5678)

      getFoo().pageViewsByTag should equal(Map(
        "politics/eu-referendum" -> Map("/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum" -> 1234),
        "profile/roberto-tyley" -> Map("/info/developer-blog/2015/feb/03/prout-is-your-pull-request-out" -> 5678)
      ))

    }
  }

  it("should update a nested map on the same tag") {
    LocalDynamoDB.usingTable(client)(table)('browserId -> S, 'userId -> S) {

      pageViewStorage.putPageView(fooLooksAtJez, 1234)

      pageViewStorage.putPageView(fooLooksAtBojo, 1024)

      getFoo().pageViewsByTag should equal(Map(
        "politics/eu-referendum" -> Map("/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum" -> 1234,
          "/politics/2016/may/17/boris-johnon-no-guarantee-vote-to-remain-will-settle-eu-issue-for-ever" -> 1024)
      ))
    }
  }
}