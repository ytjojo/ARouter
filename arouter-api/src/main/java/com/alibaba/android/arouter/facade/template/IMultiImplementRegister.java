package com.alibaba.android.arouter.facade.template;

import com.alibaba.android.arouter.facade.model.RouteMeta;

public interface IMultiImplementRegister {
  void add(Class<?> keyClass, RouteMeta routeMeta);
}
