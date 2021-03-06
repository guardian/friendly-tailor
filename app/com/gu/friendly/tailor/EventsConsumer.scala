package com.gu.friendly.tailor

import java.util.UUID

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain, InstanceProfileCredentialsProvider, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventsConsumer {

  // This application name is used by KCL to store the checkpoint data about how much of the stream you have consumed.
  val applicationName = s"${Config.stack}-${Config.app}-kinesis-${Config.stage}"

  // The Kinesis stream you want to consume from
  val streamName = "ophan-events"

  val roleArn = "arn:aws:iam::021353022223:role/membership-read-ophan-events"
  
  val initialPosition = InitialPositionInStream.LATEST

  // AWS region of the Kinesis stream
  val region = "eu-west-1"

  lazy val ophanUserCredentials: AWSCredentialsProviderChain =
    new AWSCredentialsProviderChain(
      new STSAssumeRoleSessionCredentialsProvider(roleArn, "roleSessionName"),
      new ProfileCredentialsProvider("ophan")
    )

  val defaultCredentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("membership"),
    new InstanceProfileCredentialsProvider
  )

  // Unique ID for the worker thread
  val workerId = UUID.randomUUID().toString

  val config = new KinesisClientLibConfiguration(applicationName, streamName, ophanUserCredentials, defaultCredentialsProvider, defaultCredentialsProvider, workerId)
    .withInitialPositionInStream(initialPosition)
    .withRegionName(region)

  // Create a worker, which will in turn create one or more EventProcessors
  val worker = new Worker(EventProcessorFactory, config)

  def start() = for {
    _ <- MonitoredTags.updateInterestingContent()
  } {
    MonitoredTags.start()
    worker.run()
  }

  def stop() = {
    worker.shutdown()
  }

}
