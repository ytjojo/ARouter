package com.alibaba.android.arouter.core;

import android.content.Context;

import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.facade.InterceptorResult;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.service.SyncInterceptorService;
import com.alibaba.android.arouter.facade.template.IInterceptor;

@Route(path = "/arouter/service/syncinterceptor")
public class SyncInterceptorServiceImpl implements SyncInterceptorService {


    public void init(Context context) {
    }

    @Override
    public void doInterceptions(Postcard postcard, InterceptorCallback callback) {
        int position = postcard.getPauseCause() == null ? -1 : Warehouse.interceptors.indexOf(postcard.getPauseCause());
        if (position >= 0) {
            postcard.setPauseCause(null);
        }
        _execute(position + 1, callback, postcard);
    }


    /**
     * Excute interceptor
     *
     * @param index    current interceptor index
     * @param callback InterceptorCallback
     * @param postcard routeMeta
     */
    private static void _execute(final int index, final InterceptorCallback callback, final Postcard postcard) {
        if (index < Warehouse.interceptors.size()) {
            final IInterceptor iInterceptor = Warehouse.interceptors.get(index);
            iInterceptor.process(postcard, new InterceptorCallback() {
                @Override
                public void onContinue(Postcard postcard) {
                    // Last interceptor excute over with no exception.
                    _execute(index + 1, callback, postcard);  // When counter is down, it will be execute continue ,but index bigger than interceptors size, then U know.
                }

                @Override
                public void onInterrupt(Throwable exception) {
                    // Last interceptor execute over with fatal exception.
                    Throwable throwable = null == exception ? new HandlerException("No message.") : exception;
                    postcard.setTag(throwable);    // save the exception message for backup.
                    callback.onInterrupt(throwable);
                    // Be attention, maybe the thread in callback has been changed,
                    // then the catch block(L207) will be invalid.
                    // The worst is the thread changed to main thread, then the app will be crash, if you throw this exception!
//                    if (!Looper.getMainLooper().equals(Looper.myLooper())) {    // You shouldn't throw the exception if the thread is main thread.
//                        throw new HandlerException(exception.getMessage());
//                    }
                }

                @Override
                public void onPause(Postcard postcard) {
                    callback.onPause(postcard);
                    postcard.setPauseCause(iInterceptor);
                }
            });
        } else {
            callback.onContinue(postcard);
            postcard.greenChannel();
        }
    }


}
