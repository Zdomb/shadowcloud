package com.karasiq.shadowcloud.actors

import scala.language.postfixOps

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}

import com.karasiq.shadowcloud.ShadowCloud
import com.karasiq.shadowcloud.actors.utils.ContainerActor
import com.karasiq.shadowcloud.actors.RegionContainer.SetConfig
import com.karasiq.shadowcloud.actors.RegionSupervisor.RenewRegionSubscriptions
import com.karasiq.shadowcloud.config.RegionConfig
import com.karasiq.shadowcloud.model.RegionId
import com.karasiq.shadowcloud.utils.Utils

private[actors] object RegionContainer {
  sealed trait Message
  final case class SetConfig(regionConfig: RegionConfig) extends Message

  def props(regionId: RegionId): Props = {
    Props(new RegionContainer(regionId))
  }
}

private[actors] final class RegionContainer(regionId: RegionId) extends Actor with Stash with ActorLogging with ContainerActor {
  private[this] val sc = ShadowCloud()
  var regionConfig: RegionConfig = sc.configs.regionConfig(regionId)

  def receive: Receive = {
    case SetConfig(rc) ⇒
      log.warning("Region config changed: {}", rc)
      this.regionConfig = sc.configs.regionConfig(regionId, rc)
      restartActor()
  }

  def startActor(): Unit = {
    val dispatcher = context.actorOf(RegionDispatcher.props(regionId, this.regionConfig), Utils.uniqueActorName(regionId))
    afterStart(dispatcher)
  }

  override def afterStart(actor: ActorRef): Unit = {
    sc.actors.regionSupervisor ! RenewRegionSubscriptions(regionId)
    super.afterStart(actor)
  }
}
