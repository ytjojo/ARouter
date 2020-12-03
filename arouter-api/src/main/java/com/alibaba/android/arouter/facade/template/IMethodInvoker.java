package com.alibaba.android.arouter.facade.template;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.alibaba.android.arouter.facade.Postcard;

public interface IMethodInvoker {
  Object invoke(@Nullable Context paramContext, @NonNull Postcard postcard);
}
