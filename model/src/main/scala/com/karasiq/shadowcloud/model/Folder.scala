package com.karasiq.shadowcloud.model

import scala.collection.GenTraversableOnce
import scala.language.postfixOps

import com.karasiq.shadowcloud.index.diffs.FolderDiff
import com.karasiq.shadowcloud.index.utils._

@SerialVersionUID(0L)
final case class Folder(path: Path, timestamp: Timestamp = Timestamp.now,
                        folders: Set[String] = Set.empty, files: Set[File] = Set.empty)
  extends SCEntity with HasPath with HasEmpty with HasWithoutData with HasWithoutChunks with Mergeable {

  type Repr = Folder
  type DiffRepr = FolderDiff

  def addFiles(files: GenTraversableOnce[File]): Folder = {
    val newFiles = this.files ++ files
    require(newFiles.forall(_.path.parent == this.path), "Invalid file paths")
    copy(timestamp = timestamp.modifiedNow, files = newFiles)
  }

  def addFolders(folders: GenTraversableOnce[String]): Folder = {
    copy(timestamp = timestamp.modifiedNow, folders = this.folders ++ folders)
  }

  def addFiles(files: File*): Folder = {
    addFiles(files)
  }

  def addFolders(folders: String*): Folder = {
    addFolders(folders)
  }

  def deleteFolders(folders: GenTraversableOnce[String]): Folder = {
    copy(timestamp = timestamp.modifiedNow, folders = this.folders -- folders)
  }

  def deleteFiles(files: GenTraversableOnce[File]): Folder = {
    copy(timestamp = timestamp.modifiedNow, files = this.files -- files)
  }

  def deleteFolders(folders: String*): Folder = {
    deleteFolders(folders)
  }

  def deleteFiles(files: File*): Folder = {
    deleteFiles(files)
  }

  def merge(folder: Folder): Folder = {
    require(path == folder.path, "Invalid path")
    addFolders(folder.folders).addFiles(folder.files)
  }

  def diff(oldFolder: Folder): FolderDiff = {
    require(path == oldFolder.path, "Invalid path")
    FolderDiff(oldFolder, this)
  }

  def patch(diff: FolderDiff): Folder = {
    this
      .deleteFiles(diff.deletedFiles)
      .addFiles(diff.newFiles)
      .deleteFolders(diff.deletedFolders)
      .addFolders(diff.newFolders)
      .copy(timestamp = timestamp.modified(diff.time))
  }

  def withPath(newPath: Path): Folder = {
    copy(path = newPath, files = files.map(file ⇒ file.copy(path = file.path.withParent(newPath))))
  }

  def isEmpty: Boolean = {
    files.isEmpty && folders.isEmpty
  }

  def withoutData: Folder = {
    copy(files = files.map(_.withoutData))
  }

  def withoutChunks: Folder = {
    copy(files = files.map(_.withoutChunks))
  }

  override def hashCode(): Int = {
    (path, folders, files).hashCode()
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case f: Folder ⇒
      f.path == path && f.folders == folders && f.files == files

    case _ ⇒
      false 
  }

  override def toString: String = {
    s"Folder($path, folders: [${folders.mkString(", ")}], files: [${files.mkString(", ")}])"
  }
}

object Folder {
  def create(path: Path): Folder = {
    Folder(path)
  }
}