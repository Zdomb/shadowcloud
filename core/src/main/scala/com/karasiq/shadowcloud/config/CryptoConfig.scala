package com.karasiq.shadowcloud.config

import com.karasiq.shadowcloud.config.utils.ConfigImplicits
import com.karasiq.shadowcloud.providers.CryptoProvider

private[shadowcloud] case class CryptoConfig(hashing: HashingConfig, encryption: EncryptionConfig, signing: SigningConfig, providers: ProvidersConfig[CryptoProvider])

private[shadowcloud] object CryptoConfig extends ConfigImplicits {
  def apply(config: Config): CryptoConfig = {
    CryptoConfig(
      HashingConfig(config.getConfig("hashing")),
      EncryptionConfig(config.getConfig("encryption")),
      SigningConfig(config.getConfig("signing")),
      ProvidersConfig(config.getConfig("providers"))
    )
  }
}