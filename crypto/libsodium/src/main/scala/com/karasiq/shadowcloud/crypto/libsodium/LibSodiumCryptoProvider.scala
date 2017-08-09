package com.karasiq.shadowcloud.crypto.libsodium

import scala.language.postfixOps

import com.karasiq.shadowcloud.crypto.libsodium.asymmetric.SealedBoxModule
import com.karasiq.shadowcloud.crypto.libsodium.hashing.{Blake2bModule, MultiPartHashModule}
import com.karasiq.shadowcloud.crypto.libsodium.internal.LSUtils
import com.karasiq.shadowcloud.crypto.libsodium.signing.CryptoSignModule
import com.karasiq.shadowcloud.crypto.libsodium.symmetric._
import com.karasiq.shadowcloud.providers.CryptoProvider

final class LibSodiumCryptoProvider extends CryptoProvider {
  override val hashingAlgorithms: Set[String] = ifLoaded(super.hashingAlgorithms) {
    Set("SHA256", "SHA512", "Blake2b")
  }

  override def hashing: HashingPF = ifLoaded(super.hashing) {
    case method if method.algorithm == "Blake2b" ⇒
      Blake2bModule(method)

    case method if method.algorithm == "SHA256" ⇒
      MultiPartHashModule.SHA256(method)

    case method if method.algorithm == "SHA512" ⇒
      MultiPartHashModule.SHA512(method)
  }

  override def encryptionAlgorithms: Set[String] = ifLoaded(super.encryptionAlgorithms) {
    @inline def onlyIf(cond: Boolean)(algorithms: String*): Seq[String] = if (cond) algorithms else Nil

    Set("XSalsa20/Poly1305", "ChaCha20/Poly1305", "Salsa20", "XSalsa20", "ChaCha20", SealedBoxModule.algorithm) ++
      onlyIf(LSUtils.isAesAvailable)("AES/GCM")
  }

  override def encryption: EncryptionPF = ifLoaded(super.encryption) {
    case method if method.algorithm == SealedBoxModule.algorithm ⇒
      SealedBoxModule(method)

    case method if method.algorithm == "XSalsa20/Poly1305" ⇒
      SecretBoxModule(method)

    case method if method.algorithm == "ChaCha20/Poly1305"  ⇒
      AEADCipherModule.ChaCha20_Poly1305(method)

    case method if method.algorithm == "AES/GCM" && method.keySize == 256 && LSUtils.isAesAvailable ⇒
      AEADCipherModule.AES_GCM(method)

    case method if method.algorithm == "Salsa20" ⇒
      Salsa20Module(method)

    case method if method.algorithm == "XSalsa20" ⇒
      XSalsa20Module(method)

    case method if method.algorithm == "ChaCha20" ⇒
      ChaCha20Module(method)
  }

  override def signingAlgorithms = ifLoaded(super.signingAlgorithms) {
    Set(CryptoSignModule.algorithm)
  }

  override def signing = ifLoaded(super.signing) {
    case method if method.algorithm == CryptoSignModule.algorithm ⇒
      CryptoSignModule(method)
  }

  @inline
  private[this] def ifLoaded[T](empty: ⇒ T)(value: ⇒ T): T = {
    if (LSUtils.isLibraryAvailable) value else empty
  }
}
