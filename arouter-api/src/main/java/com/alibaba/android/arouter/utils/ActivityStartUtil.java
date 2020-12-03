package com.alibaba.android.arouter.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.facade.template.IPrivateInterceptor;
import com.alibaba.android.arouter.launcher.ARouter;

import java.util.Iterator;

public class ActivityStartUtil {

    public static Activity contextToActivity(Context context) {
        if (context == null)
            return null;
        if (context instanceof Activity)
            return (Activity) context;
        if (context instanceof ContextWrapper)
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity)
                    return (Activity) context;
                context = ((ContextWrapper) context).getBaseContext();
            }
        return null;
    }

    /**
     * Start activity
     *
     * @see ActivityCompat
     */
    public static void startActivity(int requestCode, Context currentContext, Intent intent, Postcard postcard, NavigationCallback callback) {
        if (requestCode >= 0) {  // Need start for result
            if (currentContext instanceof Activity) {
                ActivityCompat.startActivityForResult((Activity) currentContext, intent, requestCode, postcard.getOptionsBundle());
            } else {
                ARouter.logger.warning(Consts.TAG, "Must use [navigation(activity, ...)] to support [startActivityForResult]");
            }
        } else {
            ActivityCompat.startActivity(currentContext, intent, postcard.getOptionsBundle());
        }

        if ((-1 != postcard.getEnterAnim() && -1 != postcard.getExitAnim()) && currentContext instanceof Activity) {    // Old version.
            ((Activity) currentContext).overridePendingTransition(postcard.getEnterAnim(), postcard.getExitAnim());
        }

        if (null != callback) { // Navigation over.
            callback.onArrival(postcard);
        }
        if (!CollectionUtils.isEmpty(postcard.getPrivateInterceptors())) {
            Iterator<IPrivateInterceptor> iterator = postcard.getPrivateInterceptors().iterator();
            while (iterator.hasNext())
                ((IPrivateInterceptor) iterator.next()).onArrival(currentContext, postcard);
        }
        GlobleCallbackNotifer.onArrival(postcard);
    }
}