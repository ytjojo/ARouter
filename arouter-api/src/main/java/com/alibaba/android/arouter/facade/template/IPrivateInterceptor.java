package com.alibaba.android.arouter.facade.template;

import android.content.Context;
import com.alibaba.android.arouter.facade.Postcard;

public interface IPrivateInterceptor extends ISyncInterceptor {
  void onArrival(Context context, Postcard postcard);
}
