package com.asfoundation.wallet.service

import android.util.Pair
import com.appcoins.wallet.billing.WalletService
import com.asfoundation.wallet.entity.Wallet
import com.asfoundation.wallet.interact.FindDefaultWalletInteract
import com.asfoundation.wallet.repository.PasswordStore
import com.asfoundation.wallet.util.WalletUtils
import ethereumj.crypto.ECKey
import io.reactivex.Single
import org.spongycastle.jcajce.provider.digest.Keccak


class AccountWalletService(private val walletInteract: FindDefaultWalletInteract,
                           private val accountKeyService: AccountKeystoreService,
                           private val passwordStore: PasswordStore) : WalletService {

  private var stringECKeyPair: Pair<String, ethereumj.crypto.ECKey>? = null

  override fun getWalletAddress(): Single<String> {
    return walletInteract.find().map { wallet -> wallet.address }
  }

  override fun signContent(content: String): Single<String> {
    return walletInteract.find()
        .flatMap { wallet -> getPrivateKey(wallet).map { ecKey -> sign(wallet.address, ecKey) } }
  }

  @Throws(Exception::class)
  fun sign(plainText: String, ecKey: ECKey): String {
    val signature = ecKey.doSign(sha3(plainText.toByteArray()))
    return signature.toHex()
  }

  private fun getPrivateKey(wallet: Wallet): Single<ECKey> {
    if (stringECKeyPair != null && stringECKeyPair!!.first.equals(wallet.address, true)) {
      return Single.just(stringECKeyPair!!.second)
    }
    return passwordStore.getPassword(wallet)
        .flatMap { password ->
          accountKeyService.exportAccount(wallet, password, password)
              .map { json ->
                ECKey.fromPrivate(WalletUtils.loadCredentials(password, json)
                    .ecKeyPair
                    .privateKey)
              }
        }.doOnSuccess { ecKey -> stringECKeyPair = Pair(wallet.address, ecKey) }
  }

  fun sha3(input: ByteArray): ByteArray {
    val kecc = Keccak.Digest256()
    kecc.update(input, 0, input.size)
    return kecc.digest()
  }
}