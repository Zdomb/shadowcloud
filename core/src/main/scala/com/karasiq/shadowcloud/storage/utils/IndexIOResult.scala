package com.karasiq.shadowcloud.storage.utils

import akka.stream.IOResult
import com.karasiq.shadowcloud.index.diffs.IndexDiff

import scala.language.postfixOps

private[shadowcloud] case class IndexIOResult[Key](key: Key, diff: IndexDiff, ioResult: IOResult)