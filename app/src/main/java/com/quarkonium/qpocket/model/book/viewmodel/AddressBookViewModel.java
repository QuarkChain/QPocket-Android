package com.quarkonium.qpocket.model.book.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.quarkonium.qpocket.api.db.address.dao.QWAddressBookDao;
import com.quarkonium.qpocket.api.db.address.table.QWAddressBook;
import com.quarkonium.qpocket.model.viewmodel.BaseAndroidViewModel;
import com.quarkonium.qpocket.MainApplication;

import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AddressBookViewModel extends BaseAndroidViewModel {

    AddressBookViewModel(MainApplication application) {
        super(application);
    }

    //*******************获取地址簿************************
    private MutableLiveData<List<QWAddressBook>> mAddress = new MutableLiveData<>();

    public void feach(int countType) {
        cancelDisposable("feach");
        Disposable disposable = Single.fromCallable(() -> {
            QWAddressBookDao dao = new QWAddressBookDao(getApplication());
            List<QWAddressBook> list;
            if (countType != -1) {
                list = dao.queryAll(countType);
            } else {
                list = dao.queryAll();
            }
            if (list != null) {
                Collections.reverse(list);
            }
            return list;
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::addressBookSuccess, v -> addressBookFail());
        addDisposable("feach", disposable);
    }

    private void addressBookSuccess(List<QWAddressBook> list) {
        mAddress.postValue(list);
    }

    private void addressBookFail() {
        mAddress.postValue(null);
    }

    public MutableLiveData<List<QWAddressBook>> addressBook() {
        return mAddress;
    }
    //*******************获取积分详情************************
}
