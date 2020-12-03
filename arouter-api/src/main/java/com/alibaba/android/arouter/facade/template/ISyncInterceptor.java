package com.alibaba.android.arouter.facade.template;

import android.content.Context;
import com.alibaba.android.arouter.facade.Postcard;

public interface ISyncInterceptor {
  boolean process(Context context, Postcard postcard);
}
