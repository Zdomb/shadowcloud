package com.karasiq.shadowcloud.actors.internal

import scala.collection.mutable
import scala.language.postfixOps

import akka.actor.{ActorContext, ActorRef}

import com.karasiq.shadowcloud.actors.events.SCEvents
import com.karasiq.shadowcloud.actors.internal.ChunksTracker.ChunkStatus
import com.karasiq.shadowcloud.config.StorageConfig
import com.karasiq.shadowcloud.index.diffs.IndexDiff
import com.karasiq.shadowcloud.storage.StorageHealth

private[actors] object StorageTracker {
  case class Storage(id: String, dispatcher: ActorRef, health: StorageHealth) {
    def config(implicit ac: ActorContext) = StorageConfig(id)
  }

  def apply()(implicit context: ActorContext): StorageTracker = {
    new StorageTracker()
  }
}

private[actors] final class StorageTracker(implicit context: ActorContext) { // TODO: Quota
  import StorageTracker._

  // -----------------------------------------------------------------------
  // State
  // -----------------------------------------------------------------------
  private[this] val events = SCEvents()
  private[this] val storages = mutable.AnyRefMap[String, Storage]()
  private[this] val storagesByAR = mutable.AnyRefMap[ActorRef, Storage]()

  // -----------------------------------------------------------------------
  // Contains
  // -----------------------------------------------------------------------
  def contains(dispatcher: ActorRef): Boolean = {
    storagesByAR.contains(dispatcher)
  }

  def contains(storageId: String): Boolean = {
    storages.contains(storageId)
  }

  // -----------------------------------------------------------------------
  // Dispatchers for read/write
  // -----------------------------------------------------------------------
  def available(toWrite: Long = 0): Seq[Storage] = {
    storagesByAR.values.toVector
      .filter(_.health.canWrite > toWrite)
      .sortBy(_.id)
  }

  def forIndexWrite(diff: IndexDiff): Seq[Storage] = {
    available(1024) // At least 1KB
  }

  def forRead(status: ChunkStatus): Seq[Storage] = {
    available().filter(s ⇒ status.hasChunk.contains(s.dispatcher))
  }

  def forWrite(chunk: ChunkStatus): Seq[Storage] = {
    def dispatcherCanWrite(dispatcher: ActorRef): Boolean = {
      !chunk.hasChunk.contains(dispatcher) &&
        !chunk.writingChunk.contains(dispatcher) &&
        !chunk.waitingChunk.contains(dispatcher)
    }
    val writeSize = chunk.chunk.checksum.encryptedSize
    available(writeSize).filter(s ⇒ dispatcherCanWrite(s.dispatcher))
  }

  // -----------------------------------------------------------------------
  // Register/unregister
  // -----------------------------------------------------------------------
  def register(storageId: String, dispatcher: ActorRef, health: StorageHealth): Unit = {
    context.watch(dispatcher)
    val storage = Storage(storageId, dispatcher, health)
    storages += storageId → storage
    storagesByAR += dispatcher → storage
    events.storage.subscribe(context.self, storageId)
  }

  def unregister(dispatcher: ActorRef): Unit = {
    context.unwatch(dispatcher)
    storagesByAR.remove(dispatcher).foreach { storage ⇒
      storages -= storage.id
      events.storage.unsubscribe(context.self, storage.id)
    }
  }

  // -----------------------------------------------------------------------
  // Get storages
  // -----------------------------------------------------------------------
  def getStorageId(dispatcher: ActorRef): String = {
    storagesByAR(dispatcher).id
  }

  def getDispatcher(storageId: String): ActorRef = {
    storages(storageId).dispatcher
  }

  // -----------------------------------------------------------------------
  // Update state
  // -----------------------------------------------------------------------
  def update(storageId: String, health: StorageHealth): Unit = {
    storages.get(storageId).foreach { storage ⇒
      val newStatus = storage.copy(health = health)
      storages += storageId → newStatus
      storagesByAR += storage.dispatcher → newStatus
    }
  }
}
