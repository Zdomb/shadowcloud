package com.karasiq.shadowcloud.crypto.index

import scala.language.postfixOps

import akka.util.ByteString

import com.karasiq.shadowcloud.config.keys.KeySet
import com.karasiq.shadowcloud.crypto._
import com.karasiq.shadowcloud.exceptions.CryptoException
import com.karasiq.shadowcloud.providers.SCModules
import com.karasiq.shadowcloud.serialization.SerializationModule
import com.karasiq.shadowcloud.serialization.protobuf.index.EncryptedIndexData

private[shadowcloud] final class IndexEncryption(modules: SCModules, keyEncryption: EncryptionMethod,
                                                 sign: SignMethod, serialization: SerializationModule) {
  private[this] val keyEncryptionModule = modules.crypto.encryptionModule(keyEncryption)
  private[this] val signModule = modules.crypto.signModule(sign)

  def encrypt(data: ByteString, method: EncryptionMethod, keys: KeySet): EncryptedIndexData = {
    val encryption = modules.crypto.encryptionModule(method)
    val parameters = encryption.createParameters()
    val encrypted = encryption.encrypt(data, parameters)
    val header = keyEncryptionModule.encrypt(serialization.toBytes(parameters), keys.encryption)
    IndexSignatures.sign(EncryptedIndexData(keys.id, header, encrypted, ByteString.empty), signModule, keys.sign)
  }

  def decrypt(data: EncryptedIndexData, keys: KeySet): ByteString = {
    if (data.keysId != keys.id || !IndexSignatures.verify(data, signModule, keys.sign)) {
      throw CryptoException.InvalidSignature()
    }
    val parameters = serialization.fromBytes[EncryptionParameters](keyEncryptionModule.decrypt(data.header, keys.encryption))
    val encryption = modules.crypto.encryptionModule(parameters.method)
    encryption.decrypt(data.data, parameters)
  }
}
