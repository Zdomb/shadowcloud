package com.karasiq.shadowcloud.model

import scala.language.postfixOps

import akka.util.ByteString

import com.karasiq.shadowcloud.index.utils.{HasEmpty, HasWithoutData}

@SerialVersionUID(0L)
final case class Data(plain: ByteString = ByteString.empty, encrypted: ByteString = ByteString.empty)
  extends SCEntity with HasEmpty with HasWithoutData {

  type Repr = Data

  def isEmpty: Boolean = {
    plain.isEmpty && encrypted.isEmpty
  }

  def withoutData: Data = {
    Data.empty
  }
}

object Data {
  val empty = Data()
}
