package com.alibaba.android.arouter.facade.template;

import com.alibaba.android.arouter.facade.model.RouteMeta;

public interface IMultiImplementsRegister {
  void add(Class<?> paramClass, RouteMeta routeMeta);
}
