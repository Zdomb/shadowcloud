package com.karasiq.shadowcloud.config

import scala.concurrent.duration.FiniteDuration

import akka.actor.ActorContext

import com.karasiq.shadowcloud.config.utils.{ChunkKeyExtractor, ConfigImplicits}

case class StorageConfig(syncInterval: FiniteDuration, indexCompactThreshold: Int, chunkKey: ChunkKeyExtractor)

object StorageConfig extends ConfigImplicits {
  def fromConfig(storageId: String, rootConfig: Config): StorageConfig = {
    apply(rootConfig.getConfigOrRef(s"storages.$storageId")
      .withFallback(rootConfig.getConfig("default-storage")))
  }

  def apply(storageId: String)(implicit ac: ActorContext): StorageConfig = {
    fromConfig(storageId, actorContextConfig(AppConfig.ROOT_CFG_PATH))
  }

  def apply(config: Config): StorageConfig = {
    StorageConfig(
      config.getFiniteDuration("sync-interval"),
      config.getInt("index-compact-threshold"),
      ChunkKeyExtractor.fromString(config.getString("chunk-key"))
    )
  }
}