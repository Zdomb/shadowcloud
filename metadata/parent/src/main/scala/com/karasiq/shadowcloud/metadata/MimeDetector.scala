package com.karasiq.shadowcloud.metadata

import akka.util.ByteString

trait MimeDetector {
  def getMimeType(name: String, data: ByteString): Option[String]
}
