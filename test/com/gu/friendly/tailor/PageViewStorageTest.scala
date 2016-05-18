package com.gu.friendly.tailor

import java.time.Instant

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
import com.gu.scanamo._
import com.gu.scanamo.syntax._
import ophan.thrift.event.AssignedId

case class StoredPageViews(browserId: String, userId: String, pageViewsByTag: Map[String, Map[String, Long]])

class PageViewStorageTest extends org.scalatest.FunSpec with org.scalatest.Matchers {

  val client = LocalDynamoDB.client()
  val pageViewStorage = new PageViewStorage(client)
  val browserFoo = AssignedId("foo")
  val fooLooksAtJez = RelevantPageView(
    "/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum",
    Set("politics/eu-referendum"),
    Instant.ofEpochMilli(1),
    browserFoo,
    None
  )
  val fooLooksAtRoberto = RelevantPageView(
    "/info/developer-blog/2015/feb/03/prout-is-your-pull-request-out",
    Set("profile/roberto-tyley"),
    Instant.ofEpochMilli(2),
    browserFoo,
    None
  )
  val fooLooksAtBojo = RelevantPageView(
    "/politics/2016/may/17/boris-johnon-no-guarantee-vote-to-remain-will-settle-eu-issue-for-ever",
    Set("politics/eu-referendum"),
    Instant.ofEpochMilli(3),
    browserFoo,
    None
  )
  val entryKey = pageViewStorage.entryKeyFor(fooLooksAtJez)
  val table = pageViewStorage.table

  def getStoredPageViewsForFoo(): StoredPageViews = {
    Scanamo.get[StoredPageViews](client)(table)('browserId -> "foo" and 'userId -> "None").get.toOption.get
  }

  it("should update a nested map") {

    LocalDynamoDB.usingTable(client)(table)('browserId -> S, 'userId -> S) {

      pageViewStorage.putPageView(fooLooksAtJez)

      getStoredPageViewsForFoo().pageViewsByTag should equal(Map("politics/eu-referendum" -> Map("/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum" -> 1L)))

      pageViewStorage.putPageView(fooLooksAtRoberto)

      getStoredPageViewsForFoo().pageViewsByTag should equal(Map(
        "politics/eu-referendum" -> Map("/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum" -> 1L),
        "profile/roberto-tyley" -> Map("/info/developer-blog/2015/feb/03/prout-is-your-pull-request-out" -> 2L)
      ))

    }
  }

  it("should update a nested map on the same tag") {
    LocalDynamoDB.usingTable(client)(table)('browserId -> S, 'userId -> S) {

      pageViewStorage.putPageView(fooLooksAtJez)
      pageViewStorage.putPageView(fooLooksAtBojo)

      getStoredPageViewsForFoo().pageViewsByTag should equal(Map(
        "politics/eu-referendum" -> Map("/politics/2016/may/13/jeremy-corbyn-young-voters-eu-referendum" -> 1L,
          "/politics/2016/may/17/boris-johnon-no-guarantee-vote-to-remain-will-settle-eu-issue-for-ever" -> 3L)
      ))
    }
  }
  it("should update an already seen page view with the latest time") {
    LocalDynamoDB.usingTable(client)(table)('browserId -> S, 'userId -> S) {

      pageViewStorage.putPageView(fooLooksAtBojo)
      pageViewStorage.putPageView(fooLooksAtBojo.copy(time = Instant.ofEpochMilli(4)))

      getStoredPageViewsForFoo().pageViewsByTag should equal(Map(
        "politics/eu-referendum" -> Map("/politics/2016/may/17/boris-johnon-no-guarantee-vote-to-remain-will-settle-eu-issue-for-ever" -> 4L)))
    }
  }
}