package com.alibaba.android.arouter.facade.template;

import android.content.Context;

import com.alibaba.android.arouter.facade.InterceptorResult;
import com.alibaba.android.arouter.facade.Postcard;

public interface ISyncInterceptor {
  InterceptorResult process(Context context, Postcard postcard);
}
