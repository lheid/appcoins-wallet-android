package com.asfoundation.wallet.poa;

import com.asf.appcoins.sdk.contractproxy.AppCoinsAddressProxySdk;
import com.asfoundation.wallet.repository.PasswordStore;
import com.asfoundation.wallet.repository.WalletRepositoryType;
import com.asfoundation.wallet.repository.Web3jProvider;
import com.asfoundation.wallet.service.AccountKeystoreService;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.math.BigDecimal;
import org.web3j.protocol.core.DefaultBlockParameterName;

public class TransactionFactory {
  private final Web3jProvider web3jProvider;
  private final WalletRepositoryType walletRepositoryType;
  private final AccountKeystoreService accountKeystoreService;
  private final PasswordStore passwordStore;
  private final DataMapper dataMapper;
  private final AppCoinsAddressProxySdk adsContractAddressSdk;

  public TransactionFactory(Web3jProvider web3jProvider, WalletRepositoryType walletRepositoryType,
      AccountKeystoreService accountKeystoreService, PasswordStore passwordStore,
      DataMapper dataMapper, AppCoinsAddressProxySdk adsContractAddressProvider) {
    this.web3jProvider = web3jProvider;
    this.walletRepositoryType = walletRepositoryType;
    this.accountKeystoreService = accountKeystoreService;
    this.passwordStore = passwordStore;
    this.dataMapper = dataMapper;
    this.adsContractAddressSdk = adsContractAddressProvider;
  }

  public Single<byte[]> createTransaction(Proof proof) {
    return adsContractAddressSdk.getAdsAddress(proof.getChainId())
        .observeOn(Schedulers.io())
        .flatMap(adsAddress -> walletRepositoryType.getDefaultWallet()
            .flatMap(wallet -> passwordStore.getPassword(wallet)
                .flatMap(
                    password -> accountKeystoreService.signTransaction(wallet.address, password,
                        adsAddress, BigDecimal.ZERO, proof.getGasPrice(), proof.getGasLimit(),
                        getNonce(wallet.address), dataMapper.getData(proof), proof.getChainId()))));
  }

  private long getNonce(String address) throws IOException {
    return web3jProvider.getDefault()
        .ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
        .send()
        .getTransactionCount()
        .longValue();
  }
}
