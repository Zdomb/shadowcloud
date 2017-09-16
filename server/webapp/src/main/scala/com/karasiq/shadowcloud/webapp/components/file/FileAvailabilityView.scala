package com.karasiq.shadowcloud.webapp.components.file

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import rx._

import com.karasiq.shadowcloud.model.File
import com.karasiq.shadowcloud.model.utils.FileAvailability
import com.karasiq.shadowcloud.webapp.components.common.{AppComponents, AppIcons}
import com.karasiq.shadowcloud.webapp.context.{AppContext, FolderContext}
import AppContext.JsExecutionContext
import com.karasiq.shadowcloud.webapp.utils.RxWithKey

object FileAvailabilityView {
  def apply(file: File)(implicit context: AppContext, folderContext: FolderContext): FileAvailabilityView = {
    new FileAvailabilityView(file)
  }

  private def getAvailabilityRx(file: File)(implicit context: AppContext, folderContext: FolderContext): Rx[FileAvailability] = {
    val availabilityRx = RxWithKey(Rx((folderContext.regionId, file, folderContext.scope())), FileAvailability(file, Map.empty)) {
      case (regionId, file, scope) ⇒
        context.api.getFileAvailability(regionId, file, scope)
    }

    availabilityRx.toRx
  }
}

class FileAvailabilityView(file: File)(implicit context: AppContext, folderContext: FolderContext) extends BootstrapHtmlComponent {
  val opened = Var(false)

  def renderTag(md: ModifierT*): TagT = {
    val content = Rx {
      if (opened()) {
        val availabilityRx = FileAvailabilityView.getAvailabilityRx(file)
        div(Rx(renderContent(availabilityRx(), md:_*)))
      } else {
        Bootstrap.noContent
      }
    }

    div(
      AppComponents.dropDownLink(context.locale.show, opened),
      content
    )
  }

  def renderContent(fileAvailability: FileAvailability, md: ModifierT*): TagT = {
    def icon(percentage: Double) = percentage match {
      case 100 ⇒
        AppIcons.fullyAvailable

      case _ ⇒
        AppIcons.partiallyAvailable
    }

    def textStyle(percentage: Double): ModifierT = percentage match {
      case 100 ⇒
        Bootstrap.textStyle.success

      case p if p > 50 ⇒
        Bootstrap.textStyle.warning

      case _ ⇒
        Bootstrap.textStyle.danger
    }

    val sortedPercentages = fileAvailability.percentagesByStorage.toSeq.sortBy(_._2)(Ordering[Double].reverse)
    div(
      for ((storageId, percentage) ← sortedPercentages)
        yield div(icon(percentage), Bootstrap.nbsp, storageId, f" ($percentage%.2f%%)", textStyle(percentage))
    )(md:_*)
  }
}
