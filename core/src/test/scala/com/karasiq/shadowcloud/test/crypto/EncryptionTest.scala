package com.karasiq.shadowcloud.test.crypto

import com.karasiq.shadowcloud.crypto.{EncryptionMethod, EncryptionModule}
import com.karasiq.shadowcloud.test.utils.TestUtils.{modules, _}
import org.scalatest.{FlatSpec, Matchers}

import scala.language.postfixOps

class EncryptionTest extends FlatSpec with Matchers {
  "Plain module" should "process data" in {
    val plainModule = modules.encryptionModule(EncryptionMethod.none)
    testModule(plainModule)
  }

  val aesMethod = EncryptionMethod("AES", 256)
  val aesModule = modules.encryptionModule(aesMethod)

  "AES module" should "generate key" in {
    val aesParameters = aesModule.createParameters().symmetric
    aesParameters.key.length shouldBe (aesMethod.keySize / 8)
    aesParameters.iv should not be empty
    println(s"Key = ${aesParameters.key.toHexString}, iv = ${aesParameters.iv.toHexString}")
  }

  it should "encrypt data" in {
    testModule(aesModule)
  }

  private[this] def testModule(module: EncryptionModule): Unit = {
    val data = randomBytes(100)
    val parameters = module.createParameters()
    val encrypted = module.encrypt(data, parameters)
    // encrypted should not be data
    encrypted.length should be >= data.length
    val decrypted = module.decrypt(encrypted, parameters)
    decrypted shouldBe data
    module.encrypt(decrypted, parameters) shouldBe encrypted // Restore
  }
}
