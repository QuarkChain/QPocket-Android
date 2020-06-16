package com.quarkonium.qpocket.api.entity;

import androidx.annotation.Nullable;

import com.quarkonium.qpocket.api.Constant;

public class ErrorEnvelope {
    public final int code;
    @Nullable
    public final String message;
    @Nullable
    public final Throwable throwable;

    public ErrorEnvelope(@Nullable String message) {
        this(Constant.ErrorCode.UNKNOWN, message);
    }

    public ErrorEnvelope(int code, @Nullable String message) {
        this(code, message, null);
    }

    public ErrorEnvelope(int code, @Nullable String message, @Nullable Throwable throwable) {
        this.code = code;
        this.message = message;
        this.throwable = throwable;
    }
}
