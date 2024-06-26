package com.alibaba.android.arouter.demo.module1.testactivity.privateInterceptor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.alibaba.android.arouter.demo.module1.MainLooper;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.template.IPrivateInterceptor;
import com.alibaba.android.arouter.launcher.ARouter;

public class TestPrivateInterceptor implements IPrivateInterceptor {
    @Override
    public void onArrival(Context context, Postcard postcard) {

    }

    @Override
    public void process(final Context context, final Postcard postcard) {

        // 这里的弹窗仅做举例，代码写法不具有可参考价值
        final AlertDialog.Builder ab = new AlertDialog.Builder(postcard.getContext());
        ab.setCancelable(false);
        ab.setTitle("温馨提醒");
        String path = postcard.getUri() != null? postcard.getUri().toString():postcard.getPath();
        ab.setMessage("想要跳转吗？(触发了\"" + path + "\"拦截器，拦截了本次跳转)");
        ab.setNegativeButton("继续", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ARouter.getInstance().resumePausedPostcard(context,"test1");
            }
        });
        ab.setNeutralButton("算了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ARouter.getInstance().removePaused("test1");
            }
        });
        ab.setPositiveButton("加点料", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                postcard.withString("extra", "我是在拦截器中附加的参数");
                ARouter.getInstance().resumePausedPostcard(context,"test1");
            }
        });

        MainLooper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ab.create().show();
            }
        });
//        ARouter.getInstance().pause("test1",postcard);
        postcard.pause("test1");

    }
}
