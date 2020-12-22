package com.alibaba.android.arouter.core;

import android.content.Context;

import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.facade.InterceptorResult;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.service.SyncInterceptorService;
import com.alibaba.android.arouter.facade.template.ISyncInterceptor;
import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.utils.CollectionUtils;

import java.util.Iterator;
import java.util.List;

@Route(path = "/arouter/service/syncinterceptor")
public class SyncInterceptorServiceImpl implements SyncInterceptorService {
    public InterceptorResult doInterceptions(Context context, Postcard postcard) {
        List<ISyncInterceptor> list = ARouter.getInstance().getMultiImplements(ISyncInterceptor.class);
        if (!CollectionUtils.isEmpty(list)) {
            Iterator<ISyncInterceptor> iterator = list.iterator();
            while (iterator.hasNext()) {
                InterceptorResult result = iterator.next().process(context, postcard);
                if (result != InterceptorResult.CONTINUE)
                    return result;
            }
        }
        return InterceptorResult.CONTINUE;
    }

    public void init(Context paramContext) {
    }

    @Override
    public void doInterceptions(Postcard postcard, InterceptorCallback callback) {
        InterceptorResult result = doInterceptions(postcard.getContext(),postcard);
        switch (result){
            case INTERRUPT:
                callback.onInterrupt(new HandlerException("The interceptor interrupt"));
                break;
            case PAUSE:
                callback.onPause(postcard);
                break;
            case CONTINUE:
                callback.onContinue(postcard);
                break;
        }
    }
}
