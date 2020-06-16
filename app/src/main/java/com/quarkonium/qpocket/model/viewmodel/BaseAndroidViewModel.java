package com.quarkonium.qpocket.model.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.entity.ErrorEnvelope;
import com.quarkonium.qpocket.api.entity.ServiceException;
import com.quarkonium.qpocket.base.SingleLiveEvent;
import com.quarkonium.qpocket.crypto.CreateWalletException;
import com.quarkonium.qpocket.crypto.KeystoreTypeException;

import java.util.HashMap;

import io.reactivex.disposables.Disposable;

//因为ViewModel的生命周期是和Activity或Fragment分开的，
// 所以在ViewModel中绝对不能引用任何View对象或者任何引用了Activity的Context的对象。
// 如果ViewModel中需要Application的Context的话，使用AndroidModel
//对应的factory为ViewModelProvider.AndroidViewModelFactory
public class BaseAndroidViewModel extends AndroidViewModel {

    protected final SingleLiveEvent<ErrorEnvelope> error = new SingleLiveEvent<>();
    protected final SingleLiveEvent<Boolean> progress = new SingleLiveEvent<>();
    private HashMap<String, Disposable> disposables = new HashMap<>();

    public BaseAndroidViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        cancel();
    }

    protected void cancel() {
        if (!disposables.isEmpty()) {
            for (Disposable disposable : disposables.values()) {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            }
        }
    }

    public LiveData<ErrorEnvelope> error() {
        return error;
    }

    public LiveData<Boolean> progress() {
        return progress;
    }

    protected void onError(Throwable throwable) {
        if (throwable instanceof ServiceException) {
            error.postValue(((ServiceException) throwable).error);
        } else if (throwable instanceof CreateWalletException) {
            error.postValue(new ErrorEnvelope(Constant.ErrorCode.WALLET_EXIT, null, throwable));
        } else if (throwable instanceof KeystoreTypeException) {
            error.postValue(new ErrorEnvelope(Constant.ErrorCode.KEYSTORE_ERROR, null, throwable));
        } else {
            error.postValue(new ErrorEnvelope(Constant.ErrorCode.UNKNOWN, null, throwable));
            Log.d("SESSION", "Err", throwable);
        }
    }

    protected void addDisposable(Disposable disposable) {
        disposables.put("normal", disposable);
    }

    protected void addDisposable(String key, Disposable disposable) {
        disposables.put(key, disposable);
    }

    protected void cancelDisposable(String key) {
        Disposable disposable = disposables.get(key);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
