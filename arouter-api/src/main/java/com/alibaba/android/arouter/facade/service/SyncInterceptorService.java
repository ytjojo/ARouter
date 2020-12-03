package com.alibaba.android.arouter.facade.service;

import android.content.Context;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.template.IProvider;

public interface SyncInterceptorService extends IProvider {
  boolean doInterceptions(Context context, Postcard postcard);
}
