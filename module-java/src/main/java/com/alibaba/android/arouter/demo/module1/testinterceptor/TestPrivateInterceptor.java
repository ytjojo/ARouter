package com.alibaba.android.arouter.demo.module1.testinterceptor;

import android.content.Context;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.template.IPrivateInterceptor;

public class TestPrivateInterceptor implements IPrivateInterceptor {
    @Override
    public void onArrival(Context context, Postcard postcard) {

    }

    @Override
    public boolean process(Context context, Postcard postcard) {
        return false;
    }
}
