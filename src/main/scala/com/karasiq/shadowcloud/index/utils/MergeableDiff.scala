package com.karasiq.shadowcloud.index.utils

import scala.language.postfixOps

trait MergeableDiff extends Mergeable {
  type Repr
  type DiffRepr = Repr
  def reverse: Repr
  def creates: Repr
  def deletes: Repr

  override def patch(diff: Repr): Repr = {
    merge(diff)
  }
}
