package com.karasiq.shadowcloud.dropbox

import scala.concurrent.ExecutionContext

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import com.dropbox.core.v2.files.ListFolderErrorException

import com.karasiq.dropbox.client.DropboxClient
import com.karasiq.shadowcloud.model.Path
import com.karasiq.shadowcloud.storage.StorageIOResult
import com.karasiq.shadowcloud.storage.repository.PathTreeRepository
import com.karasiq.shadowcloud.storage.utils.StorageUtils

object DropboxRepository {
  def apply(dropboxClient: DropboxClient)(implicit ec: ExecutionContext): DropboxRepository = {
    new DropboxRepository(dropboxClient)
  }
}

class DropboxRepository(dropboxClient: DropboxClient)(implicit ec: ExecutionContext) extends PathTreeRepository {
  def subKeys(fromPath: Path) = {
    require(Path.isConventional(fromPath), s"Non-conventional: $fromPath")
    dropboxClient.list(fromPath.toString, recursive = true)
      .map(metadata ⇒ Path.fromString(metadata.getPathDisplay))
      .alsoToMat(StorageUtils.countPassedElements(fromPath).toMat(Sink.head)(Keep.right))(Keep.right)
      .recoverWithRetries(1, { case e: ListFolderErrorException if e.errorValue.getPathValue.isNotFound ⇒ Source.empty })
      .map(_.toRelative(fromPath))
  }

  def read(path: Path) = {
    require(Path.isConventional(path), s"Non-conventional: $path")
    dropboxClient.download(path.toString)
      .alsoToMat(StorageUtils.countPassedBytes(path).toMat(Sink.head)(Keep.right))(Keep.right)
  }

  def write(path: Path) = {
    require(Path.isConventional(path), s"Non-conventional: $path")
    Flow[ByteString]
      .toMat(dropboxClient.upload(path.toString))(Keep.right)
      .mapMaterializedValue { future ⇒
        val resultFuture = future.map { fm ⇒
          require(Path.equalsIgnoreCase(fm.getPathDisplay, path), "File renamed")
          StorageIOResult.Success(path, fm.getSize)
        }
        StorageUtils.wrapFuture(path, resultFuture)
      }
  }

  def delete = {
    Flow[Path]
      .mapAsyncUnordered(2) { path ⇒
        require(Path.isConventional(path), s"Non-conventional: $path")
        dropboxClient.delete(path.toString)
      }
      .toMat(StorageUtils.countPassedElements().toMat(Sink.head)(Keep.right))(Keep.right)
  }
}
