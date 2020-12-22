package com.alibaba.android.arouter.demo.module1.testinterceptor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.alibaba.android.arouter.demo.module1.MainLooper;
import com.alibaba.android.arouter.facade.InterceptorResult;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.template.IPrivateInterceptor;
import com.alibaba.android.arouter.launcher.ARouter;

public class TestPrivateInterceptor implements IPrivateInterceptor {
    @Override
    public void onArrival(Context context, Postcard postcard) {

    }

    @Override
    public InterceptorResult process(final Context context, final Postcard postcard) {
        if(System.currentTimeMillis()%2 ==0){
            return InterceptorResult.INTERRUPT;
        }
        // 这里的弹窗仅做举例，代码写法不具有可参考价值
        final AlertDialog.Builder ab = new AlertDialog.Builder(postcard.getContext());
        ab.setCancelable(false);
        ab.setTitle("温馨提醒");
        ab.setMessage("想要跳转到Test4Activity么？(触发了\"/inter/test1\"拦截器，拦截了本次跳转)");
        ab.setNegativeButton("继续", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ARouter.getInstance().resumePausePostCard(context,"test1");
            }
        });
        ab.setNeutralButton("算了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ARouter.getInstance().removePause("test1");
            }
        });
        ab.setPositiveButton("加点料", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                postcard.withString("extra", "我是在拦截器中附加的参数");
                ARouter.getInstance().resumePausePostCard(context,"test1");
            }
        });

        MainLooper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ab.create().show();
            }
        });
        ARouter.getInstance().pause("test1",postcard);
        return InterceptorResult.PAUSE;
    }
}
