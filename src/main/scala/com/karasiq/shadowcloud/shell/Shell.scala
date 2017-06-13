package com.karasiq.shadowcloud.shell

import java.nio.file.Files

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.postfixOps

import com.karasiq.shadowcloud.actors.RegionSupervisor
import com.karasiq.shadowcloud.config.RegionConfig
import com.karasiq.shadowcloud.storage.props.StorageProps

object Shell extends ImplicitConversions {
  private[this] implicit val context = ShellContext()

  import context._
  import sc.actors.regionSupervisor

  def init(): Unit = {
    val testDirectory = {
      sys.props
        .get("shadowcloud.temp-storage-dir")
        .fold(Files.createTempDirectory("sc-shell"))(toFSPath)
    }

    // TODO: sc.start() function
    sc.password.masterPassword // Request password
    sc.actors.regionSupervisor // Start actor

    val state = Await.result(sc.ops.supervisor.getState(), Duration.Inf)
    if (!state.regions.contains("test") && !state.storages.contains("test")) {
      val testRegion = createRegion("test")
      val testStorage = createStorage("test", StorageProps.fromDirectory(testDirectory))
      testRegion.register(testStorage)
    }

    println(sc.keys.provider.forEncryption())
  }

  def openRegion(regionId: String): RegionContext = {
    RegionContext(regionId)
  }

  def createRegion(regionId: String): RegionContext = {
    regionSupervisor ! RegionSupervisor.AddRegion(regionId, RegionConfig.forId(regionId, actorSystem.settings.config.getConfig("shadowcloud")))
    openRegion(regionId)
  }

  def openStorage(storageId: String): StorageContext = {
    StorageContext(storageId)
  }

  def createStorage(storageId: String, props: StorageProps): StorageContext = {
    regionSupervisor ! RegionSupervisor.AddStorage(storageId, props)
    openStorage(storageId)
  }

  def createTempStorage(storageId: String): StorageContext = {
    createStorage(storageId, StorageProps.fromDirectory(Files.createTempDirectory("sc-shell")))
  }

  def test(): Unit = {
    val testRegion = openRegion("test")
    val testStorage = openStorage("test")

    testStorage.sync()
    Thread.sleep(5000)

    println("Uploading file")
    val testFile = "LICENSE"
    testRegion.upload(testFile, testFile)
    testStorage.compactIndex(testRegion.regionId)
    testStorage.sync()
    Thread.sleep(5000)

    println("Downloading file")
    Files.deleteIfExists(testFile + "_remote")
    testRegion.download(testFile + "_remote", testFile)

    println("Deleting file")
    testRegion.deleteFile(testFile)
    testStorage.sync()
    Thread.sleep(5000)

    testStorage.collectGarbage(delete = true)
  }

  def quit(): Unit = {
    Await.result(actorSystem.terminate(), Duration.Inf)
    sys.exit()
  }
}
