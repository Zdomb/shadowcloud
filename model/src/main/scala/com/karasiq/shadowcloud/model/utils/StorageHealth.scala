package com.karasiq.shadowcloud.model.utils

import com.karasiq.common.memory.MemorySize

@SerialVersionUID(0L)
final case class StorageHealth(writableSpace: Long, totalSpace: Long, usedSpace: Long = 0L, online: Boolean = true) extends HealthStatus {
  require(writableSpace >= 0 && totalSpace >= 0 && usedSpace >= 0, "Invalid sizes")

  def freeSpace: Long = {
    math.max(0L, totalSpace - usedSpace)
  }

  def +(h1: StorageHealth): StorageHealth = {
    copy(writableSpace + h1.writableSpace, totalSpace + h1.totalSpace, usedSpace + h1.usedSpace, online && h1.online)
  }

  def -(bytes: Long): StorageHealth = {
    StorageHealth.normalized(writableSpace - bytes, totalSpace, usedSpace + bytes, online)
  }

  override def toString: String = {
    s"StorageHealth(${if (online) "" else "Offline, "}${MemorySize(writableSpace)} available, ${MemorySize(usedSpace)}/${MemorySize(totalSpace)})"
  }
}

object StorageHealth {
  val empty = StorageHealth(0, 0, 0)
  val unlimited = StorageHealth(Long.MaxValue, Long.MaxValue, 0)

  def normalized(writableSpace: Long, totalSpace: Long, usedSpace: Long = 0L, online: Boolean = true): StorageHealth = {
    val totalSpaceN = if (totalSpace >= 0) totalSpace else Long.MaxValue
    val usedSpaceN = if (usedSpace >= 0) math.min(totalSpaceN, usedSpace) else totalSpaceN
    val writableSpaceN = math.max(0L, math.min(totalSpaceN - usedSpaceN, writableSpace))

    new StorageHealth(
      writableSpaceN,
      totalSpaceN,
      usedSpaceN,
      online
    )
  }
}
