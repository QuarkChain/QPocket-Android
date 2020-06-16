package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.model.lock.LockPatternActivity;
import com.quarkonium.qpocket.model.main.AddTokenActivity;
import com.quarkonium.qpocket.model.main.MainActivity;
import com.quarkonium.qpocket.model.main.TokenListActivity;
import com.quarkonium.qpocket.model.main.WalletEditActivity;
import com.quarkonium.qpocket.model.main.WalletManagerActivity;
import com.quarkonium.qpocket.model.splash.SplashActivity;
import com.quarkonium.qpocket.model.transaction.MergeActivity;
import com.quarkonium.qpocket.model.transaction.OtherTokenDetailActivity;
import com.quarkonium.qpocket.model.transaction.PublicSaleDetailActivity;
import com.quarkonium.qpocket.model.transaction.PublicSaleTransactionCreateActivity;
import com.quarkonium.qpocket.model.transaction.TRXFreezeActivity;
import com.quarkonium.qpocket.model.transaction.TransactionCreateActivity;
import com.quarkonium.qpocket.model.transaction.TransactionDetailActivity;
import com.quarkonium.qpocket.model.transaction.TransactionDetailCostActivity;
import com.quarkonium.qpocket.model.transaction.TransactionPublicDetailCostActivity;
import com.quarkonium.qpocket.model.transaction.TransactionSendActivity;
import com.quarkonium.qpocket.model.wallet.BackupPhraseActivity;
import com.quarkonium.qpocket.model.wallet.BackupPhraseHintActivity;
import com.quarkonium.qpocket.model.wallet.BackupPhraseInputActivity;
import com.quarkonium.qpocket.model.wallet.CreateChildAccountActivity;
import com.quarkonium.qpocket.model.wallet.CreateWalletActivity;
import com.quarkonium.qpocket.model.wallet.ImportWalletFragment;
import com.quarkonium.qpocket.view.MergeEditPopWindow;
import com.quarkonium.qpocket.model.book.AddressBookActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class BuildersModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = SplashModule.class)
    abstract SplashActivity bindSplashModule();

    @ActivityScope
    @ContributesAndroidInjector(modules = CreateWalletModule.class)
    abstract CreateWalletActivity bindCreateWalletModule();

    @ActivityScope
    @ContributesAndroidInjector(modules = CreateWalletModule.class)
    abstract BackupPhraseHintActivity bindBackupPhraseHintActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = CreateWalletModule.class)
    abstract BackupPhraseActivity bindBackupPhraseActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = CreateWalletModule.class)
    abstract BackupPhraseInputActivity bindBackupPhraseInputActivity();

    @FragmentScope
    @ContributesAndroidInjector(modules = ImportWalletModule.class)
    abstract ImportWalletFragment bindImportWalletFragment();

    @ActivityScope
    @ContributesAndroidInjector(modules = MainWalletModule.class)
    abstract MainActivity bindMainActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = EditWalletModule.class)
    abstract WalletEditActivity bindWalletEditActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = ManagerWalletModule.class)
    abstract WalletManagerActivity bindWalletManagerActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract TransactionDetailActivity bindTransactionDetailActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract TransactionDetailCostActivity bindTransactionDetailCostActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract TransactionCreateActivity bindTransactionCreateActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract TransactionSendActivity bindTransactionSendActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract PublicSaleTransactionCreateActivity bindPublicSaleTransactionCreateActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract OtherTokenDetailActivity bindOtherTokenDetailActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = PublicSaleDetailModule.class)
    abstract PublicSaleDetailActivity bindPublicSaleDetailActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract MergeActivity bindMergeActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = MainWalletModule.class)
    abstract AddTokenActivity bindAddTokenActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = MainWalletModule.class)
    abstract LockPatternActivity bindLockPatternActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = MainWalletModule.class)
    abstract TokenListActivity bindTokenListActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract TransactionPublicDetailCostActivity bindTransactionPublicDetailCostActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract TRXFreezeActivity bindTRXFreezeActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = AddressBookModule.class)
    abstract AddressBookActivity bindAddressBookActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = ImportWalletModule.class)
    abstract CreateChildAccountActivity bindCreateChildAccountActivity();

    @FragmentScope
    @ContributesAndroidInjector(modules = TransactionModule.class)
    abstract MergeEditPopWindow bindMergeEditPopWindow();
}
