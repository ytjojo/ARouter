package com.alibaba.android.arouter.launcher;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.android.arouter.core.InstrumentationHook;
import com.alibaba.android.arouter.core.LogisticsCenter;
import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.exception.InitException;
import com.alibaba.android.arouter.exception.NoRouteFoundException;
import com.alibaba.android.arouter.facade.InterceptorResult;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.alibaba.android.arouter.facade.service.*;
import com.alibaba.android.arouter.facade.template.ILogger;
import com.alibaba.android.arouter.facade.template.IMethodInvoker;
import com.alibaba.android.arouter.facade.template.IPrivateInterceptor;
import com.alibaba.android.arouter.facade.template.IRouteGroup;
import com.alibaba.android.arouter.thread.DefaultPoolExecutor;
import com.alibaba.android.arouter.utils.ActivityStartUtil;
import com.alibaba.android.arouter.utils.CollectionUtils;
import com.alibaba.android.arouter.utils.Consts;
import com.alibaba.android.arouter.utils.DefaultLogger;
import com.alibaba.android.arouter.utils.GlobleCallbackNotifer;
import com.alibaba.android.arouter.utils.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ARouter core (Facade patten)
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/16 14:39
 */
final class _ARouter {
    static ILogger logger = new DefaultLogger(Consts.TAG);
    private volatile static boolean monitorMode = false;
    private volatile static boolean debuggable = false;
    private volatile static boolean autoInject = false;
    private volatile static _ARouter instance = null;
    private volatile static boolean hasInit = false;
    private volatile static ThreadPoolExecutor executor = DefaultPoolExecutor.getInstance();
    private HashMap<String, Postcard> mPausedPostcards = new HashMap<String, Postcard>();
    private static Handler mHandler;
    private static Context mContext;

    private static InterceptorService interceptorService;

    private _ARouter() {
    }

    protected static synchronized boolean init(Application application) {
        mContext = application;
        LogisticsCenter.init(mContext, executor);
        logger.info(Consts.TAG, "ARouter init success!");
        hasInit = true;
        mHandler = new Handler(Looper.getMainLooper());

        return true;
    }

    /**
     * Destroy arouter, it can be used only in debug mode.
     */
    static synchronized void destroy() {
        if (debuggable()) {
            hasInit = false;
            LogisticsCenter.suspend();
            logger.info(Consts.TAG, "ARouter destroy success!");
        } else {
            logger.error(Consts.TAG, "Destroy can be used in debug mode only!");
        }
    }

    protected static _ARouter getInstance() {
        if (!hasInit) {
            throw new InitException("ARouterCore::Init::Invoke init(context) first!");
        } else {
            if (instance == null) {
                synchronized (_ARouter.class) {
                    if (instance == null) {
                        instance = new _ARouter();
                    }
                }
            }
            return instance;
        }
    }

    static synchronized void openDebug() {
        debuggable = true;
        logger.info(Consts.TAG, "ARouter openDebug");
    }

    static synchronized void openLog() {
        logger.showLog(true);
        logger.info(Consts.TAG, "ARouter openLog");
    }

    @Deprecated
    static synchronized void enableAutoInject() {
        autoInject = true;
    }

    @Deprecated
    static boolean canAutoInject() {
        return autoInject;
    }

    @Deprecated
    static void attachBaseContext() {
        Log.i(Consts.TAG, "ARouter start attachBaseContext");
        try {
            Class<?> mMainThreadClass = Class.forName("android.app.ActivityThread");

            // Get current main thread.
            Method getMainThread = mMainThreadClass.getDeclaredMethod("currentActivityThread");
            getMainThread.setAccessible(true);
            Object currentActivityThread = getMainThread.invoke(null);

            // The field contain instrumentation.
            Field mInstrumentationField = mMainThreadClass.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);

            // Hook current instrumentation
            mInstrumentationField.set(currentActivityThread, new InstrumentationHook());
            Log.i(Consts.TAG, "ARouter hook instrumentation success!");
        } catch (Exception ex) {
            Log.e(Consts.TAG, "ARouter hook instrumentation failed! [" + ex.getMessage() + "]");
        }
    }

    static synchronized void printStackTrace() {
        logger.showStackTrace(true);
        logger.info(Consts.TAG, "ARouter printStackTrace");
    }

    static synchronized void setExecutor(ThreadPoolExecutor tpe) {
        executor = tpe;
    }

    static synchronized void monitorMode() {
        monitorMode = true;
        logger.info(Consts.TAG, "ARouter monitorMode on");
    }

    static boolean isMonitorMode() {
        return monitorMode;
    }

    static boolean debuggable() {
        return debuggable;
    }

    static void setLogger(ILogger userLogger) {
        if (null != userLogger) {
            logger = userLogger;
        }
    }

    static void inject(Object thiz) {
        AutowiredService autowiredService = ((AutowiredService) ARouter.getInstance().build("/arouter/service/autowired").navigation());
        if (null != autowiredService) {
            autowiredService.autowire(thiz);
        }
    }

    /**
     * Build postcard by path and default group
     */
    protected Postcard build(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new HandlerException(Consts.TAG + "Parameter is invalid!");
        } else {
            PathReplaceService pService = ARouter.getInstance().navigation(PathReplaceService.class);
            if (null != pService) {
                path = pService.forString(path);
            }
            return build(path, extractGroup(path), true);
        }
    }

    /**
     * Build postcard by uri
     */
    protected Postcard build(Uri uri) {
        if (null == uri || TextUtils.isEmpty(uri.toString())) {
            throw new HandlerException(Consts.TAG + "Parameter invalid!");
        } else {
            PathReplaceService pService = ARouter.getInstance().navigation(PathReplaceService.class);
            if (null != pService) {
                uri = pService.forUri(uri);
            }
            return new Postcard(uri.getPath(), extractGroup(uri.getPath()), uri, null);
        }
    }

    /**
     * Build postcard by path and group
     */
    protected Postcard build(String path, String group, Boolean afterReplace) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(group)) {
            throw new HandlerException(Consts.TAG + "Parameter is invalid!");
        } else {
            if (!afterReplace) {
                PathReplaceService pService = ARouter.getInstance().navigation(PathReplaceService.class);
                if (null != pService) {
                    path = pService.forString(path);
                }
            }
            return new Postcard(path, group);
        }
    }

    /**
     * Extract the default group from path.
     */
    private String extractGroup(String path) {
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new HandlerException(Consts.TAG + "Extract the default group failed, the path must be start with '/' and contain more than 2 '/'!");
        }

        try {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (TextUtils.isEmpty(defaultGroup)) {
                throw new HandlerException(Consts.TAG + "Extract the default group failed! There's nothing between 2 '/'!");
            } else {
                return defaultGroup;
            }
        } catch (Exception e) {
            logger.warning(Consts.TAG, "Failed to extract default group! " + e.getMessage());
            return null;
        }
    }

    static void afterInit() {
        // Trigger interceptor init, use byName.
        interceptorService = (InterceptorService) ARouter.getInstance().build("/arouter/service/interceptor").navigation();
    }

    protected <T> T navigation(Class<? extends T> service) {
        try {
            Postcard postcard = LogisticsCenter.buildProvider(service);

            // Compatible 1.0.5 compiler sdk.
            // Earlier versions did not use the fully qualified name to get the service
            if (null == postcard) {
                // No service, or this service in old version.
                postcard = LogisticsCenter.buildProvider(service);
            }

            if (null == postcard) {
                return null;
            }

            // Set application to postcard.
            postcard.setContext(mContext);

            LogisticsCenter.completion(postcard);
            return (T) postcard.getProvider();
        } catch (NoRouteFoundException ex) {
            logger.warning(Consts.TAG, ex.getMessage());
            return null;
        }
    }

    /**
     * Use router navigation.
     *
     * @param context     Activity or null.
     * @param postcard    Route metas
     * @param requestCode RequestCode
     * @param callback    cb
     */
    protected Object navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        PretreatmentService pretreatmentService = ARouter.getInstance().navigation(PretreatmentService.class);
        if (null != pretreatmentService && !pretreatmentService.onPretreatment(context, postcard)) {
            // Pretreatment failed, navigation canceled.
            return null;
        }

        // Set context to postcard.
        postcard.setContext(null == context ? mContext : context);
        postcard.setRequestCode(requestCode);
        postcard.setNavigationCallback(callback);

        try {
            LogisticsCenter.completion(postcard);
        } catch (NoRouteFoundException ex) {
            logger.warning(Consts.TAG, ex.getMessage());

            if (debuggable()) {
                // Show friendly tips for user.
                runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "There's no route matched!\n" +
                                " Path = [" + postcard.getPath() + "]\n" +
                                " Group = [" + postcard.getGroup() + "]", Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (null != callback) {
                callback.onLost(postcard);
            } else {
                // No callback for this invoke, then we use the global degrade service.
                List<DegradeService> degradeSevices = ARouter.getInstance().getMultiImplements(DegradeService.class);
                if (!CollectionUtils.isEmpty(degradeSevices)) {
                    for (DegradeService degradeService : degradeSevices) {
                        if (degradeService.onLost(postcard.getContext(), postcard)) {
                            break;
                        }
                    }
                }
            }
            GlobleCallbackNotifer.onLost(postcard);

            return null;
        }

        if (null != callback) {
            callback.onFound(postcard);
        }
        GlobleCallbackNotifer.onFound(postcard);
        if (postcard.isForIntent()) {
            postcard.greenChannel();
        }
        return intercept(context, postcard, requestCode, callback);
    }

    private Object intercept(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        if(context != null && context != postcard.getContext()){
            postcard.setContext(context);
        }

        if (!postcard.isGreenChannel() && LogisticsCenter.hasInterceptors()) {   // It must be run in async thread, maybe interceptor cost too mush time made ANR.
            interceptorService.doInterceptions(postcard, new InterceptorCallback() {
                /**
                 * Continue process
                 *
                 * @param postcard route meta
                 */
                @Override
                public void onContinue(final Postcard postcard) {
                    runInMainThread(new Runnable() {
                        @Override
                        public void run() {
                            _navigation(postcard, requestCode, callback);
                        }
                    });

                }

                /**
                 * Interrupt process, pipeline will be destory when this method called.
                 *
                 * @param exception Reson of interrupt.
                 */
                @Override
                public void onInterrupt(Throwable exception) {
                    if (callback != null) {
                        callback.onInterrupt(postcard);
                    }
                    GlobleCallbackNotifer.onInterrupt(postcard);
                    logger.info(Consts.TAG, "Navigation failed, termination by interceptor : " + exception.getMessage());
                }

                @Override
                public void onPause(Postcard postcard) {
                    logger.info(Consts.TAG, "Navigation failed, termination by interceptor ");
                }
            });
        } else {
            return _navigation(postcard, requestCode, callback);
        }
        return null;
    }

    private Object _navigation(final Postcard postcard, final int requestCode, final NavigationCallback callback) {

        if (isPaused(postcard)) {
            return null;
        } else {
            InterceptorResult result = LogisticsCenter.doPrivateInterceptions(postcard);
            if (result != InterceptorResult.CONTINUE) {
                return null;
            }
        }
        final Context currentContext = postcard.getContext();

        switch (postcard.getType()) {
            case ACTIVITY:
                postcard.buildIntent(currentContext);
                if (postcard.isForIntent()) {
                    return postcard.getIntent();
                }
                // Navigation in main looper.
                runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        ActivityStartUtil.startActivity(requestCode, currentContext, postcard.getIntent(), postcard, callback);
                    }
                });

                break;
            case PROVIDER:
                return postcard.getProvider();
            case BOARDCAST:
            case CONTENT_PROVIDER:
            case FRAGMENT:
                Class<?> fragmentMeta = postcard.getDestination();
                try {
                    Object instance = fragmentMeta.getConstructor().newInstance();
                    if (instance instanceof Fragment) {
                        ((Fragment) instance).setArguments(postcard.getExtras());
                    } else if (instance instanceof androidx.fragment.app.Fragment) {
                        ((androidx.fragment.app.Fragment) instance).setArguments(postcard.getExtras());
                    }

                    return instance;
                } catch (Exception ex) {
                    logger.error(Consts.TAG, "Fetch fragment instance error, " + TextUtils.formatStackTrace(ex.getStackTrace()));
                }
            case METHOD:
                return invokeMethodInternal(postcard, postcard.getNavigationCallback());
            case SERVICE:
            default:
                return null;
        }

        return null;
    }

    /**
     * Be sure execute in main thread.
     *
     * @param runnable code
     */
    private void runInMainThread(Runnable runnable) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            mHandler.post(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Start activity
     *
     * @see ActivityCompat
     */
    private void startActivity(int requestCode, Context currentContext, Intent intent, Postcard postcard, NavigationCallback callback) {
        if (requestCode >= 0) {  // Need start for result
            if (currentContext instanceof Activity) {
                ActivityCompat.startActivityForResult((Activity) currentContext, intent, requestCode, postcard.getOptionsBundle());
            } else {
                logger.warning(Consts.TAG, "Must use [navigation(activity, ...)] to support [startActivityForResult]");
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
    }

    boolean addRouteGroup(IRouteGroup group) {
        if (null == group) {
            return false;
        }

        String groupName = null;

        try {
            // Extract route meta.
            Map<String, RouteMeta> dynamicRoute = new HashMap<>();
            group.loadInto(dynamicRoute);

            // Check route meta.
            for (Map.Entry<String, RouteMeta> route : dynamicRoute.entrySet()) {
                String path = route.getKey();
                String groupByExtract = extractGroup(path);
                RouteMeta meta = route.getValue();

                if (null == groupName) {
                    groupName = groupByExtract;
                }

                if (null == groupName || !groupName.equals(groupByExtract) || !groupName.equals(meta.getGroup())) {
                    // Group name not consistent
                    return false;
                }
            }

            LogisticsCenter.addRouteGroupDynamic(groupName, group);

            logger.info(Consts.TAG, "Add route group [" + groupName + "] finish, " + dynamicRoute.size() + " new route meta.");

            return true;
        } catch (Exception exception) {
            logger.error(Consts.TAG, "Add route group dynamic exception! " + exception.getMessage());
        }

        return false;
    }


    protected <T> List<T> getMultiImplements(Class<? extends T> paramClass) {
        return LogisticsCenter.getMultiImplements(paramClass);
    }

    protected Object invokeMethodInternal(Postcard postcard, NavigationCallback navigationCallback) {
        Context context = postcard.getContext();
        if (context == null) {
            context = mContext;
        }
        IMethodInvoker iMethodInvoker = LogisticsCenter.getIInvokeMethod((Class<IMethodInvoker>) postcard.getDestination());
        LogisticsCenter.createPrivateInterceptors(postcard);
        Object object = iMethodInvoker.invoke(context, postcard);
        if (navigationCallback != null) {
            navigationCallback.onArrival(postcard);
        }
        if (!CollectionUtils.isEmpty(postcard.getPrivateInterceptors())) {
            for (IPrivateInterceptor interceptor : postcard.getPrivateInterceptors()) {
                interceptor.onArrival(postcard.getContext(), postcard);
            }
        }
        GlobleCallbackNotifer.onArrival(postcard);
        return object;
    }

    protected void pause(String tag, Postcard postcard) {
        this.mPausedPostcards.put(tag, postcard);
        if (postcard.getNavigationCallback() != null) {
            postcard.getNavigationCallback().onPause(postcard);
        }
        GlobleCallbackNotifer.onPause(postcard);
    }

    protected Object invokeMethod(Context paramContext, Postcard postcard, NavigationCallback paramNavigationCallback) {
        return navigation(paramContext, postcard, -1, paramNavigationCallback);
    }

    protected boolean isPaused(Postcard postcard) {
        return (!this.mPausedPostcards.isEmpty() && this.mPausedPostcards.values().contains(postcard));
    }

    protected <T> T navigationWithTemplate(Class<? extends T> templateClass) {
        try {
            return (T) LogisticsCenter.buildTemplateImpl(templateClass);
        } catch (Exception exception) {
            logger.warning("ARouter::", exception.getMessage());
            return null;
        }
    }

    protected void putRoute(String path, RouteMeta routemeta) {
        LogisticsCenter.putRoute(path, routemeta);
    }

    protected boolean removePaused(String tag) {
        Postcard postcard = this.mPausedPostcards.remove(tag);
        if (postcard != null) {
            onInterrupt(null, postcard);
            return true;
        }
        return false;

    }

    protected boolean removePaused(Postcard postcard) {
        for (Map.Entry<String, Postcard> entry : mPausedPostcards.entrySet()) {
            if (entry.getValue() == postcard) {
                mPausedPostcards.remove(entry.getKey());
                break;
            }
        }
        if (postcard != null) {
            onInterrupt(null, postcard);
            return true;
        }
        return false;

    }

    protected void onInterrupt(Object exception, Postcard postcard) {
        if (exception != null) {
            postcard.setTag(exception);
        }
        if (postcard.getNavigationCallback() != null) {
            postcard.getNavigationCallback().onInterrupt(postcard);
        }
        GlobleCallbackNotifer.onInterrupt(postcard);
    }

    protected void onInterrupt(Object exception, String tag) {
        if (exception == null) {
            exception = new HandlerException("No message.");
        }
        final Postcard postcard = getPausedPostcard(tag);
        if (removePaused(tag)) {
            onInterrupt(exception, postcard);
        }
    }

    protected Postcard getPausedPostcard(String tag) {
        return mPausedPostcards.get(tag);
    }

    protected void resumePausedPostcard(Context context, String tag) {
        Postcard postcard = this.mPausedPostcards.remove(tag);
        if (postcard == null) {
            logger.error(Consts.TAG, "resumePausePostcard with tag " + tag + " not fonnd");
            return;
        }
        if(postcard.getType() == null || postcard.getDestination() == null){
            navigation(context,postcard,postcard.getRequestCode(),postcard.getNavigationCallback());
        }else {
            intercept(context, postcard, postcard.getRequestCode(), postcard.getNavigationCallback());
        }
    }

}
