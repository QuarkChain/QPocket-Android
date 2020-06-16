package com.quarkonium.qpocket.model.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.entity.ErrorEnvelope;
import com.quarkonium.qpocket.api.entity.ServiceException;

import io.reactivex.disposables.Disposable;

//因为ViewModel的生命周期是和Activity或Fragment分开的，
// 所以在ViewModel中绝对不能引用任何View对象或者任何引用了Activity的Context的对象。
// 如果ViewModel中需要Application的Context的话，使用AndroidModel
public class BaseViewModel extends ViewModel {

    protected final MutableLiveData<ErrorEnvelope> error = new MutableLiveData<>();
    protected final MutableLiveData<Boolean> progress = new MutableLiveData<>();
    protected Disposable disposable;
    protected Disposable disposable2;

    @Override
    protected void onCleared() {
        cancel();
    }

    protected void cancel() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        if (disposable2 != null && !disposable2.isDisposed()) {
            disposable2.dispose();
        }
    }

    public LiveData<ErrorEnvelope> error() {
        return error;
    }

    public LiveData<Boolean> progress() {
        return progress;
    }

    protected void onError(Throwable throwable) {
//        Crashlytics.logException(throwable);
        if (throwable instanceof ServiceException) {
            error.postValue(((ServiceException) throwable).error);
        } else {
            error.postValue(new ErrorEnvelope(Constant.ErrorCode.UNKNOWN, null, throwable));
            Log.d("SESSION", "Err", throwable);
        }
    }
}
