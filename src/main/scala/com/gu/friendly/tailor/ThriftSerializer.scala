package com.gu.friendly.tailor

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import com.twitter.scrooge.ThriftStruct
import org.apache.thrift.TBaseHelper
import org.apache.thrift.protocol.{TCompactProtocol, TProtocol}
import org.apache.thrift.transport.{TIOStreamTransport, TMemoryInputTransport}

object ThriftSerializer {
  private val protocolFactory = new TCompactProtocol.Factory()

  def inputProtocolFrom(byteBuffer: ByteBuffer): TProtocol =
    inputProtocolFrom(TBaseHelper.byteBufferToByteArray(byteBuffer))

  def inputProtocolFrom(bytes: Array[Byte]): TProtocol =
    protocolFactory.getProtocol(new TMemoryInputTransport(bytes))

  def fromByteBuffer[T <: ThriftStruct](byteBuffer: ByteBuffer)(decoder: TProtocol => T): T =
    decoder(inputProtocolFrom(byteBuffer))

  def asThriftEncodedBytes[T <: ThriftStruct](thriftObject: T, initialBufferSizeHint: Int = 2000): Array[Byte] = {
    val byteArrayOutputStream = new ByteArrayOutputStream(initialBufferSizeHint)
    val writer = new TIOStreamTransport(byteArrayOutputStream)
    val proto = protocolFactory.getProtocol(writer)

    thriftObject.write(proto)

    writer.flush()

    byteArrayOutputStream.toByteArray
  }
}
