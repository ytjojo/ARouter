package com.alibaba.android.arouter.facade.template;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.Postcard;

public interface IMethodInvoker {
  Object invoke(@Nullable Context context, @NonNull Postcard postcard);
}
