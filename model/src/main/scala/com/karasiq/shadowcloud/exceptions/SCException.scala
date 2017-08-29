package com.karasiq.shadowcloud.exceptions

import java.io.FileNotFoundException
import java.nio.file.FileAlreadyExistsException

import com.karasiq.shadowcloud.index.{Chunk, Path}
import com.karasiq.shadowcloud.index.diffs.IndexDiff

abstract class SCException(message: String = null, cause: Throwable = null) extends Exception(message, cause)

object SCException {
  trait WrappedError
  trait IOError
  trait NotFound
  trait AlreadyExists

  trait ChunkAssociated {
    def chunk: Chunk
  }

  trait DiffAssociated {
    def diff: IndexDiff
  }

  trait PathAssociated {
    def path: Path
  }

  def isNotFound(error: Throwable): Boolean = error match {
    case _: NotFound | _: FileNotFoundException | _: NoSuchElementException ⇒
      true

    case wrapped: WrappedError if wrapped.getCause != error ⇒
      isNotFound(wrapped.getCause)

    case _ ⇒
      false
  }

  def isAlreadyExists(error: Throwable): Boolean = error match {
    case _: AlreadyExists | _: FileAlreadyExistsException ⇒
      true

    case wrapped: WrappedError if wrapped.getCause != error ⇒
      isAlreadyExists(wrapped.getCause)

    case _ ⇒
      false
  }
}
