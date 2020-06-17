# QPocket

[<img src=https://github.com/QuarkChain/QPocket-Android/blob/master/img/en_badge_web_generic.png height="88">](https://play.google.com/store/apps/details?id=com.quarkonium.qpocket)

Welcome to QPocket, a standalone blockchain payment app for your Android device!

## Introduction
QPocket is a blockchain wallet app. The wallet app follows the concept of HD wallet, and can be derived through a set of mnemonic words to support unlimited sub-wallet accounts under the public chain.  The wallet currently supports four public chains, QKC, ETH, TRX, and BTC.

## Building
To build everything from source, simply checkout the source and build using gradle on the build system you need:

 * JDK 1.8

Then you need to use the Android SDK manager to install the following components:

 * `ANDROID_HOME` environment variable pointing to the directory where the SDK is installed
 * Android SDK Tools 30.0.2
 * Android SDK build Tools 30.0.0
 * NDK 21.3.0
 * Android 6.0 (API 23)
 * Android Extras:
    * AndroidX Support Library
    * Google Play services for Froyo rev 12
    * Google Play services rev 17

The project layout is designed to be used with a recent version of Android Studio (currently 4.0)

#### Build commands

To get the source code, type:

    git clone https://github.com/QuarkChain/QPocket-Android.git
    cd wallet-android

Features
========

With the QPocket Wallet you can send and receive QuarkChain,Ethereum,Tron,Bitcoins using your mobile phone.

 - HD enabled - manage multiple accounts and never reuse addresses ([Bip32](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki)/[Bip44](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki) compatible)
 - Masterseed based - make one backup and be safe for ever. ([Bip39](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki))
 - 100% control over your private keys, they never leave your device unless you export them
 - Watch-only addresses (single or xPub) & private key import for secure cold-storage integration
 - Secure your wallet with a PIN

Please note: while we make sure to adhere to the highest standards of software craftsmanship we can not exclude that the software contains bugs. Please make sure you have backups of your private keys and do not use this for more than you are willing to lose.


More features:
 - Sources [available for review](https://github.com/QuarkChain/QPocket-Android)
 - Multiple HD accounts, private keys,keystore accounts or external xPub accounts
 - HD Wallet supports creating unlimited sub wallets
 - View your balance in multiple fiat currencies: USD, AUD, CAD, CHF, CNY, DKK, EUR, GBP, HKD, JPY, NZD, PLN, RUB, SEK, SGD, THB, and many more
 - Address book for commonly used addresses
 - Transaction history with detailed information and local stored comments
 - Export private-, keystore- or mnemonic, on clipboard or share with other applications
 - Integrated QR-code scanner
 - Sign Messages using your private keys


## Thanks and more info
Thanks web3j, ethereum and others library.

