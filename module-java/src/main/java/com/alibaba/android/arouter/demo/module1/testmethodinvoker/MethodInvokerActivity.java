package com.alibaba.android.arouter.demo.module1.testmethodinvoker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

public class MethodInvokerActivity extends AppCompatActivity {
    protected void onCreate(@Nullable Bundle paramBundle) {
        super.onCreate(paramBundle);
        startActivity((Intent) ARouter.getInstance().build("/test/getintent").invokeMethodWithReturn((Context) this));
    }

    @Route(path = "/test/methodInvoker")
    public static void start(Context context) {
        context.startActivity(new Intent(context,MethodInvokerActivity.class));
    }
}