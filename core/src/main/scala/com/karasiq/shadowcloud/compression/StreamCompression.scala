package com.karasiq.shadowcloud.compression

import akka.NotUsed
import akka.stream.scaladsl.{Compression, Flow, Source}
import akka.util.ByteString

import com.karasiq.shadowcloud.compression.lz4.LZ4Streams

object StreamCompression {
  object CompressionType extends Enumeration {
    val none = Value(0, "none")
    val gzip = Value(1, "gzip")
    val lz4 = Value(2, "lz4")
  }

  def compress(compressionType: CompressionType.Value): Flow[ByteString, ByteString, NotUsed] = {
    assert(compressionType.id < 256)
    Flow[ByteString]
      .via(createCompressionStream(compressionType))
      .prepend(Source.single(ByteString(compressionType.id.toByte)))
  }

  def decompress: Flow[ByteString, ByteString, NotUsed] = {
    Flow[ByteString].dropWhile(_.isEmpty).prefixAndTail(1).flatMapConcat { case (head +: Nil, stream) ⇒
      val dataStream = Source.single(head.drop(1)).concat(stream)
      val compType = CompressionType(java.lang.Byte.toUnsignedInt(head.head))
      dataStream.via(createDecompressionStream(compType))
    }
  }

  private[this] def createCompressionStream(compressionType: CompressionType.Value) = compressionType match {
    case CompressionType.none ⇒
      Flow[ByteString]

    case CompressionType.gzip ⇒
      Compression.gzip

    case CompressionType.lz4 ⇒
      LZ4Streams.compress
  }

  private[this] def createDecompressionStream(compressionType: CompressionType.Value) = compressionType match {
    case CompressionType.none ⇒
      Flow[ByteString]

    case CompressionType.gzip ⇒
      Compression.gunzip()

    case CompressionType.lz4 ⇒
      LZ4Streams.decompress
  }
}
