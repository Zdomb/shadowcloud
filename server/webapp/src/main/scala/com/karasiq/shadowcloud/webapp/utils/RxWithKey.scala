package com.karasiq.shadowcloud.webapp.utils

import scala.concurrent.{ExecutionContext, Future}

import rx.{Ctx, Rx, Var}

object RxWithKey {
  def static[K, V](initialKey: K, initialValue: V)(getValue: K ⇒ Future[V])(implicit ctx: Ctx.Owner, ec: ExecutionContext): RxWithKey[K, V] = {
    new RxWithKey(initialKey, initialValue, getValue)
  }

  def apply[K, V](keyRx: Rx[K], initialValue: V)(getValue: K ⇒ Future[V])(implicit ctx: Ctx.Owner, ec: ExecutionContext): RxWithKey[K, V] = {
    val result = static(keyRx.now, initialValue)(getValue)
    keyRx.foreach(result.update)
    result
  }
}

class RxWithKey[K, V](initialKey: K, initialValue: V, getValue: K ⇒ Future[V])
                     (implicit ctx: Ctx.Owner, ec: ExecutionContext) extends HasUpdate with HasKeyUpdate[K] {
  
  final val counter = Var(0)
  final val key = Var(initialKey)
  protected final val value = Var(initialValue)

  counter.triggerLater {
    getValue(key.now).foreach(value() = _)
  }

  key.trigger {
    this.update()
  }

  def toRx: Rx[V] = {
    value
  }

  def update(newKey: K): Unit = {
    key() = newKey
  }

  def update(): Unit = {
    counter() = counter.now + 1
  }
}
