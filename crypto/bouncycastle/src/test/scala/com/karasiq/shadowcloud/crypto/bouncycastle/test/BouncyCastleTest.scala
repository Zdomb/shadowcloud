package com.karasiq.shadowcloud.crypto.bouncycastle.test

import java.security.NoSuchAlgorithmException

import akka.util.ByteString
import com.karasiq.shadowcloud.config.ConfigProps
import com.karasiq.shadowcloud.crypto.bouncycastle.BouncyCastleCryptoProvider
import com.karasiq.shadowcloud.crypto.bouncycastle.hashing.{BCDigests, MessageDigestModule}
import com.karasiq.shadowcloud.crypto.bouncycastle.symmetric.{AEADBlockCipherModule, StreamCipherModule}
import com.karasiq.shadowcloud.crypto.{EncryptionModule, HashingMethod, HashingModule}
import com.karasiq.shadowcloud.utils.HexString
import org.scalatest.{FlatSpec, Matchers}

import scala.language.postfixOps

class BouncyCastleTest extends FlatSpec with Matchers {
  val provider = new BouncyCastleCryptoProvider
  val testData = ByteString("# First, make a nonce: A single-use value never repeated under the same key\n# The nonce isn't secret, and can be sent with the ciphertext.\n# The cipher instance has a nonce_bytes method for determining how many bytes should be in a nonce")

  // -----------------------------------------------------------------------
  // Encryption
  // -----------------------------------------------------------------------
  testEncryption("AES/GCM", AEADBlockCipherModule.AES_GCM(), 32, 12)
  testEncryption("Salsa20", StreamCipherModule.Salsa20(), 32, 8)
  testEncryption("XSalsa20", StreamCipherModule.XSalsa20(), 32, 24)
  testEncryption("ChaCha20", StreamCipherModule.ChaCha20(), 32, 8)

  // -----------------------------------------------------------------------
  // Hashes
  // -----------------------------------------------------------------------
  val testHashes = Seq(
    "1b1ecddca9aeacf61e56b059ebba904d7556f2f7708298ad50666b297d982726",
    "5ce1b271c63e92ded87414a5f54a990f56f77d67796b98fc512e8f77542547b9",
    "4e895c898f8e528b015765b5fe249f6a",
    "990b4eccd8294682ea0d1f49c6e61c3d",
    "b6edb34b60a98bcd9272359b27065ea2",
    "9092d8cdf95fdf6e863204b844b48f88acf74414",
    "4fbca748243adc8c6e9f303d54be9adc",
    "a28e33efe405a4d8680e09688bd4227964b03da4",
    "67d64b31980590f553806a43a4a7946b2d9f0aa99430afeb6992d2f03df29caf",
    "cd5a67223ed15d87c1f5bfba5226256a5578126bff0106c2e8e6743204df64306324277d5e1519a8",
    "d6d7ec52fbbb5ca8fc5a1cf32e91cf5fcdb3940bf3ce9fc02e4c93ed",
    "e3fc39605cd8e9245ed8cb41e2730c940e6026b9d2f72ead3b0f2d271e2290e0", // SHA-256
    "34f9d73f9797df99ea73247512ddfe93a518791a3fa1d00cdf5760a801b63a013e7fa9086b64907740d8bf9930a1d9c4",
    "11bba64289c2fefc6caf753cc14fd3b914663f0035b0e2135bb29fc5159f9e99ddc57c577146688f4b64cfae09d9be933c22b17eb4a08cdb92e2c1d68efa0f59", // SHA-512
    "25a9624ad4a99cc0af46f37211d76aea591ba166ba66d68a4307195389f6fcf2",
    "a07288c021ff5342cdaf3947a327124ca91bf9008e1de6ef91befed0a91a202b",
    "510da5764778db12d7c094b86ac0f7aaf0a18eca4cedc0048dfd25f2039f3eea",
    "655cbea331b81708a8e1392ae0a5ba54c558f440b3a254ee",
    "44444854782341ac2e2e8076a8a97880d5d2f83e90f087544af94315d71c585d65f68a58174ede4e34ccb4daae71ff60ab3232a6ec521704b8560cb0b6472688",
    "824396f4585a22b2c4b36df76f55e669d4edfb423970071b6b616ce454a95400" // Blake2b
  )

  "Test hashes" should "exist" in {
    BCDigests.algorithms.length shouldBe testHashes.length
  }

  BCDigests.algorithms.zip(testHashes).foreach { case (alg, hash) ⇒
    testHashing(alg, hash)
  }

  testHashing(MessageDigestModule.forMDWithSize("Blake2b", HashingMethod("Blake2b-512", config = ConfigProps("digest-size" → 512))),
    "9f84251be0c325ad771696302e9ed3cd174f84ffdd0b8de49664e9a3ea934b89a4d008581cd5803b80b3284116174b3c4a79a5029996eb59edc1fbacfd18204e")
  testHashing(MessageDigestModule.forMDWithTwoSizes("Skein", HashingMethod("Skein-1024-1024", config = ConfigProps("state-size" → 1024, "digest-size" → 1024))),
    "66588dbe2beb3b9cea762f42e3abaa9dc406bfa005fed3579089d8d2c5807453aa6cb0f8e69134ad47405c843e9a08c51da931827957f06ca58b3e8fe658993e" +
      "1ca87d19a09bc168cc5845bc3235050f8dd59c8f8ec302bbdff4508b16c1c7cef694e1a4c84c250132d445637e0a84772196162a5815c38e45ff3dac4374f567")

  // -----------------------------------------------------------------------
  // Tests specification
  // -----------------------------------------------------------------------
  private[this] def testEncryption(name: String, module: EncryptionModule, keySize: Int, nonceSize: Int): Unit = {
    s"$name module" should "generate key" in {
      val parameters = module.createParameters()
      parameters.symmetric.key.length shouldBe keySize
      parameters.symmetric.nonce.length shouldBe nonceSize
      val parameters1 = module.updateParameters(parameters)
      parameters1.symmetric.nonce should not be parameters.symmetric.nonce
    }

    it should "encrypt data" in {
      val parameters = module.createParameters()
      val encrypted = module.encrypt(testData, parameters)
      encrypted.length should be >= testData.length
      val decrypted = module.decrypt(encrypted, parameters)
      decrypted shouldBe testData
    }
  }

  private[this] def testHashing(name: String, testHash: String): Unit = {
    try {
      val module = provider.hashing(HashingMethod(name))
      testHashing(module, testHash)
    } catch {
      case _: NoSuchAlgorithmException ⇒
        println(s"No such algorithm: $name")
    }
  }

  private[this] def testHashing(module: HashingModule, testHash: String): Unit = {
    s"${module.method.algorithm} module" should "generate hash" in {
      val hash = HexString.encode(module.createHash(testData))
      // println('"' + hash + '"' + ",")
      hash shouldBe testHash
    }
  }
}