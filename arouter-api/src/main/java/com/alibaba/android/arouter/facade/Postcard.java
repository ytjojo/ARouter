package com.alibaba.android.arouter.facade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityOptionsCompat;
import android.util.SparseArray;

import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.alibaba.android.arouter.facade.service.SerializationService;
import com.alibaba.android.arouter.facade.template.IPrivateInterceptor;
import com.alibaba.android.arouter.facade.template.IProvider;
import com.alibaba.android.arouter.facade.util.ArrayUtils;
import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.utils.TextUtils;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A container that contains the roadmap.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.1.0
 * @since 16/8/22 19:16
 */
public final class Postcard extends RouteMeta {
    // Base
    private Uri uri;
    private Object tag;             // A tag prepare for some thing wrong. inner params, DO NOT USE!
    private Bundle mBundle;         // Data to transform
    private int flags = 0;         // Flags of route
    private int timeout = 300;      // Navigation timeout, TimeUnit.Second
    private IProvider provider;     // It will be set value, if this postcard was provider.
    private boolean greenChannel;
    private SerializationService serializationService;
    private Context context;        // May application or activity, check instance type before use it.
    private String action;

    // Animation
    private Bundle optionsCompat;    // The transition animation of activity
    private int enterAnim = -1;
    private int exitAnim = -1;


    private WeakReference<Context> contextRef;

    private Intent intent;

    private Uri intentData;

    private Class<?> keyClass;

    private NavigationCallback navigationCallback;

    private HashMap<String, Object> objectHashMap;


    private ArrayList<IPrivateInterceptor> privateInterceptors;

    private Object pauseCause;


    private int requestCode = -1;

    private boolean isForIntent;


    public Bundle getOptionsBundle() {
        return optionsCompat;
    }

    public int getEnterAnim() {
        return enterAnim;
    }

    public int getExitAnim() {
        return exitAnim;
    }

    public IProvider getProvider() {
        return provider;
    }

    public Postcard setProvider(IProvider provider) {
        this.provider = provider;
        return this;
    }

    public Postcard() {
        this(null, null);
    }

    public Postcard(String path, String group, Class<?> keyClass) {
        this(path, group, (Uri) null, (Bundle) null, keyClass);
    }

    public Postcard(String path, String group) {
        this(path, group, null, null, null);
    }

    public Postcard(String path, String group, Uri uri, Bundle bundle) {
        this(path, group, uri, bundle, null);
    }

    public Postcard(String path, String group, Uri uri, Bundle bundle, Class keyClass) {
        setPath(path);
        setGroup(group);
        setUri(uri);
        this.mBundle = (null == bundle ? new Bundle() : bundle);
        this.keyClass = keyClass;
    }

    public boolean isGreenChannel() {
        return greenChannel;
    }

    public Object getTag() {
        return tag;
    }

    public Postcard setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    public Bundle getExtras() {
        return mBundle;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Set timeout of navigation this time.
     *
     * @param timeout timeout
     * @return this
     */
    public Postcard setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public Uri getUri() {
        return uri;
    }

    public Postcard setUri(Uri uri) {
        this.uri = uri;
        return this;
    }

    /**
     * Navigation to the route with path in postcard.
     * No param, will be use application context.
     */
    public Object navigation() {
        return navigation(null);
    }

    /**
     * Navigation to the route with path in postcard.
     *
     * @param context Activity and so on.
     */
    public Object navigation(Context context) {
        return navigation(context, null);
    }

    /**
     * Navigation to the route with path in postcard.
     *
     * @param context Activity and so on.
     */
    public Object navigation(Context context, NavigationCallback callback) {
        return ARouter.getInstance().navigation(context, this, -1, callback);
    }

    /**
     * Navigation to the route with path in postcard.
     *
     * @param mContext    Activity and so on.
     * @param requestCode startActivityForResult's param
     */
    public void navigation(Activity mContext, int requestCode) {
        navigation(mContext, requestCode, null);
    }

    /**
     * Navigation to the route with path in postcard.
     *
     * @param mContext    Activity and so on.
     * @param requestCode startActivityForResult's param
     */
    public void navigation(Activity mContext, int requestCode, NavigationCallback callback) {
        ARouter.getInstance().navigation(mContext, this, requestCode, callback);
    }

    /**
     * Green channel, it will skip all of interceptors.
     *
     * @return this
     */
    public Postcard greenChannel() {
        this.greenChannel = true;
        return this;
    }

    /**
     * BE ATTENTION TO THIS METHOD WAS <P>SET, NOT ADD!</P>
     */
    public Postcard with(Bundle bundle) {
        if (null != bundle) {
            mBundle = bundle;
        }

        return this;
    }

    /**
     * Set special flags controlling how this intent is handled.  Most values
     * here depend on the type of component being executed by the Intent,
     * specifically the FLAG_ACTIVITY_* flags are all for use with
     * {@link Context#startActivity Context.startActivity()} and the
     * FLAG_RECEIVER_* flags are all for use with
     * {@link Context#sendBroadcast(Intent) Context.sendBroadcast()}.
     */
    public Postcard withFlags(int flag) {
        this.flags = flag;
        return this;
    }

    /**
     * Add additional flags to the intent (or with existing flags
     * value).
     *
     * @param flags The new flags to set.
     * @return Returns the same Intent object, for chaining multiple calls
     * into a single statement.
     * @see #withFlags
     */
    public Postcard addFlags(int flags) {
        this.flags |= flags;
        return this;
    }

    public int getFlags() {
        return flags;
    }

    /**
     * Set object value, the value will be convert to string by 'Fastjson'
     *
     * @param key   a String, or null
     * @param value a Object, or null
     * @return current
     */
    public Postcard withObject(@Nullable String key, @Nullable Object value) {
        serializationService = ARouter.getInstance().navigation(SerializationService.class);
        mBundle.putString(key, serializationService.object2Json(value));
        return this;
    }

    // Follow api copy from #{Bundle}

    /**
     * Inserts a String value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a String, or null
     * @return current
     */
    public Postcard withString(@Nullable String key, @Nullable String value) {
        mBundle.putString(key, value);
        return this;
    }

    /**
     * Inserts a Boolean value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a boolean
     * @return current
     */
    public Postcard withBoolean(@Nullable String key, boolean value) {
        mBundle.putBoolean(key, value);
        return this;
    }

    /**
     * Inserts a short value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key   a String, or null
     * @param value a short
     * @return current
     */
    public Postcard withShort(@Nullable String key, short value) {
        mBundle.putShort(key, value);
        return this;
    }

    /**
     * Inserts an int value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key   a String, or null
     * @param value an int
     * @return current
     */
    public Postcard withInt(@Nullable String key, int value) {
        mBundle.putInt(key, value);
        return this;
    }

    /**
     * Inserts a long value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key   a String, or null
     * @param value a long
     * @return current
     */
    public Postcard withLong(@Nullable String key, long value) {
        mBundle.putLong(key, value);
        return this;
    }

    /**
     * Inserts a double value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key   a String, or null
     * @param value a double
     * @return current
     */
    public Postcard withDouble(@Nullable String key, double value) {
        mBundle.putDouble(key, value);
        return this;
    }

    /**
     * Inserts a byte value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key   a String, or null
     * @param value a byte
     * @return current
     */
    public Postcard withByte(@Nullable String key, byte value) {
        mBundle.putByte(key, value);
        return this;
    }

    /**
     * Inserts a char value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key   a String, or null
     * @param value a char
     * @return current
     */
    public Postcard withChar(@Nullable String key, char value) {
        mBundle.putChar(key, value);
        return this;
    }

    /**
     * Inserts a float value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key   a String, or null
     * @param value a float
     * @return current
     */
    public Postcard withFloat(@Nullable String key, float value) {
        mBundle.putFloat(key, value);
        return this;
    }

    /**
     * Inserts a CharSequence value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a CharSequence, or null
     * @return current
     */
    public Postcard withCharSequence(@Nullable String key, @Nullable CharSequence value) {
        mBundle.putCharSequence(key, value);
        return this;
    }

    /**
     * Inserts a Parcelable value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a Parcelable object, or null
     * @return current
     */
    public Postcard withParcelable(@Nullable String key, @Nullable Parcelable value) {
        mBundle.putParcelable(key, value);
        return this;
    }

    /**
     * Inserts an array of Parcelable values into the mapping of this Bundle,
     * replacing any existing value for the given key.  Either key or value may
     * be null.
     *
     * @param key   a String, or null
     * @param value an array of Parcelable objects, or null
     * @return current
     */
    public Postcard withParcelableArray(@Nullable String key, @Nullable Parcelable[] value) {
        mBundle.putParcelableArray(key, value);
        return this;
    }

    /**
     * Inserts a List of Parcelable values into the mapping of this Bundle,
     * replacing any existing value for the given key.  Either key or value may
     * be null.
     *
     * @param key   a String, or null
     * @param value an ArrayList of Parcelable objects, or null
     * @return current
     */
    public Postcard withParcelableArrayList(@Nullable String key, @Nullable ArrayList<? extends Parcelable> value) {
        mBundle.putParcelableArrayList(key, value);
        return this;
    }

    /**
     * Inserts a SparceArray of Parcelable values into the mapping of this
     * Bundle, replacing any existing value for the given key.  Either key
     * or value may be null.
     *
     * @param key   a String, or null
     * @param value a SparseArray of Parcelable objects, or null
     * @return current
     */
    public Postcard withSparseParcelableArray(@Nullable String key, @Nullable SparseArray<? extends Parcelable> value) {
        mBundle.putSparseParcelableArray(key, value);
        return this;
    }

    /**
     * Inserts an ArrayList value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value an ArrayList object, or null
     * @return current
     */
    public Postcard withIntegerArrayList(@Nullable String key, @Nullable ArrayList<Integer> value) {
        mBundle.putIntegerArrayList(key, value);
        return this;
    }

    /**
     * Inserts an ArrayList value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value an ArrayList object, or null
     * @return current
     */
    public Postcard withStringArrayList(@Nullable String key, @Nullable ArrayList<String> value) {
        mBundle.putStringArrayList(key, value);
        return this;
    }

    /**
     * Inserts an ArrayList value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value an ArrayList object, or null
     * @return current
     */
    public Postcard withCharSequenceArrayList(@Nullable String key, @Nullable ArrayList<CharSequence> value) {
        mBundle.putCharSequenceArrayList(key, value);
        return this;
    }

    /**
     * Inserts a Serializable value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a Serializable object, or null
     * @return current
     */
    public Postcard withSerializable(@Nullable String key, @Nullable Serializable value) {
        mBundle.putSerializable(key, value);
        return this;
    }

    /**
     * Inserts a byte array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a byte array object, or null
     * @return current
     */
    public Postcard withByteArray(@Nullable String key, @Nullable byte[] value) {
        mBundle.putByteArray(key, value);
        return this;
    }

    /**
     * Inserts a short array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a short array object, or null
     * @return current
     */
    public Postcard withShortArray(@Nullable String key, @Nullable short[] value) {
        mBundle.putShortArray(key, value);
        return this;
    }

    /**
     * Inserts a char array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a char array object, or null
     * @return current
     */
    public Postcard withCharArray(@Nullable String key, @Nullable char[] value) {
        mBundle.putCharArray(key, value);
        return this;
    }

    /**
     * Inserts a float array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a float array object, or null
     * @return current
     */
    public Postcard withFloatArray(@Nullable String key, @Nullable float[] value) {
        mBundle.putFloatArray(key, value);
        return this;
    }

    /**
     * Inserts a CharSequence array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a CharSequence array object, or null
     * @return current
     */
    public Postcard withCharSequenceArray(@Nullable String key, @Nullable CharSequence[] value) {
        mBundle.putCharSequenceArray(key, value);
        return this;
    }

    /**
     * Inserts a Bundle value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key   a String, or null
     * @param value a Bundle object, or null
     * @return current
     */
    public Postcard withBundle(@Nullable String key, @Nullable Bundle value) {
        mBundle.putBundle(key, value);
        return this;
    }

    /**
     * Set normal transition anim
     *
     * @param enterAnim enter
     * @param exitAnim  exit
     * @return current
     */
    public Postcard withTransition(int enterAnim, int exitAnim) {
        this.enterAnim = enterAnim;
        this.exitAnim = exitAnim;
        return this;
    }

    /**
     * Set options compat
     *
     * @param compat compat
     * @return this
     */
    @RequiresApi(16)
    public Postcard withOptionsCompat(ActivityOptionsCompat compat) {
        if (null != compat) {
            this.optionsCompat = compat.toBundle();
        }
        return this;
    }


    public Intent buildIntent(Context context) {
        Intent intent = new Intent(context, getDestination());
        intent.putExtras(getExtras());
        if (getIntentData() != null) {
            intent.setData(getIntentData());
        }
        int flags = getFlags();
        if (0 != flags) {
            intent.setFlags(flags);
        } else if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        String action = getAction();
        if (!TextUtils.isEmpty(action)) {
            intent.setAction(action);
        }
        setIntent(intent);
        return getIntent();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else {
            if (other instanceof Postcard) {
                Postcard postcard = (Postcard) other;
                return getPath().equals(postcard.getPath()) && getGroup().equals(postcard.getGroup());
            }
        }
        return false;
    }


    public Context getContext() {
        WeakReference<Context> weakReference = this.contextRef;
        return (weakReference != null) ? weakReference.get() : null;
    }

    public Intent getIntent() {
        return this.intent;
    }

    public Uri getIntentData() {
        Uri uri = this.intentData;
        return (uri == null) ? this.uri : uri;
    }

    public Postcard withIntentData(Uri uri) {
        this.intentData = uri;
        return this;
    }

    public Class<?> getKeyClass() {
        return this.keyClass;
    }

    public NavigationCallback getNavigationCallback() {
        return this.navigationCallback;
    }

    public Object getObject(String key) {
        HashMap<String, Object> hashMap = this.objectHashMap;
        return (hashMap != null) ? hashMap.get(key) : null;
    }

    public ArrayList<IPrivateInterceptor> getPrivateInterceptors() {
        return this.privateInterceptors;
    }

    public int getRequestCode() {
        return this.requestCode;
    }


    public void pause(String tag) {
        ARouter.getInstance().pause(tag, this);
    }
    public void resumePausePostCard(Context context,String tag){
        ARouter.getInstance().resumePausedPostcard(context,tag);
    }

    public void removePausedd(){
        ARouter.getInstance().removePaused(this);
    }

    public int hashCode() {
        return ArrayUtils.hashCode(new Object[]{getPath(), getGroup()});
    }

    public Object invokeMethod() {
        return invokeMethod(null);
    }

    public Object invokeMethod(Context postcard) {
        return invokeMethod(postcard, null);
    }


    public Object invokeMethod(Context context, NavigationCallback callback) {
        return ARouter.getInstance().invokeMethod(context, this, callback);
    }


    public void setContext(Context context) {
        this.contextRef = new WeakReference<Context>(context);
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public void setNavigationCallback(NavigationCallback paramNavigationCallback) {
        this.navigationCallback = paramNavigationCallback;
    }

    public void setPrivateInterceptors(ArrayList<IPrivateInterceptor> paramArrayList) {
        this.privateInterceptors = paramArrayList;
    }

    public void setRequestCode(int paramInt) {
        this.requestCode = paramInt;
    }


    public Object getPauseCause() {
        return pauseCause;
    }

    public void setPauseCause(Object pauseCause) {
        this.pauseCause = pauseCause;
    }

    @Override
    public String toString() {
        return "Postcard{" +
                "uri=" + uri +
                ", tag=" + tag +
                ", mBundle=" + mBundle +
                ", flags=" + flags +
                ", timeout=" + timeout +
                ", provider=" + provider +
                ", greenChannel=" + greenChannel +
                ", optionsCompat=" + optionsCompat +
                ", enterAnim=" + enterAnim +
                ", exitAnim=" + exitAnim +
                "}\n" +
                super.toString();
    }

    public String getAction() {
        return action;
    }

    public Postcard withAction(String action) {
        this.action = action;
        return this;
    }

    public Postcard setForIntent() {
        isForIntent = true;
        return this;
    }

    public boolean isForIntent() {
        return isForIntent;
    }

    public void interrupt(Throwable throwable) {
        setTag(throwable == null ? new HandlerException("No message.") : throwable);
    }


}
