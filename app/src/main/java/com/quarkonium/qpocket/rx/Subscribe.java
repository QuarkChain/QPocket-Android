package com.quarkonium.qpocket.rx;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Rxbus
 * Created by bhm on 2018/5/15.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {
    int code() default -1;

    ThreadMode threadMode() default ThreadMode.CURRENT_THREAD;
}
