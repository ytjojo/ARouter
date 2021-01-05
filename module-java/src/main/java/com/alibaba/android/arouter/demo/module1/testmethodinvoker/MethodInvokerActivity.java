package com.alibaba.android.arouter.demo.module1.testmethodinvoker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.alibaba.android.arouter.demo.module1.testinterceptor.Test1Interceptor;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Action;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Flags;
import com.alibaba.android.arouter.facade.annotation.Query;
import com.alibaba.android.arouter.facade.annotation.RequestCode;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

public class MethodInvokerActivity extends AppCompatActivity {
    @Autowired(name = "name")
    String userName;

    protected void onCreate(@Nullable Bundle paramBundle) {
        super.onCreate(paramBundle);
        ARouter.getInstance().inject(this);
        startActivity((Intent) ARouter.getInstance().build("/test/getintent").invokeMethod((Context) this));
    }

    @Route(path = "/test/methodInvoker", interceptors = {Test1Interceptor.class})
    public static void start(Context context, @Query("name") String userName) {
        Intent intent = new Intent(context, MethodInvokerActivity.class);
        intent.putExtra("name", userName);
        context.startActivity(intent);
    }

    @Route(path = "/test/methodInvoker1", interceptors = {Test1Interceptor.class})
    public static void startWithAcitonFlag(Activity context, @Query("name") String userName, @Action("defaultAction") String action, @Flags int flag, @RequestCode int requestCode) {
        Intent intent = new Intent(context, MethodInvokerActivity.class);
        intent.putExtra("name", userName);
        intent.setAction(action);
        if (flag != 0) {
            intent.setFlags(flag);
        }
        context.startActivityForResult(intent, requestCode);
    }
}