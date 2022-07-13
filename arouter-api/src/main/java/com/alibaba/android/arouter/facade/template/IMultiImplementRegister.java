package com.alibaba.android.arouter.facade.template;

import com.alibaba.android.arouter.facade.model.RouteMeta;

public interface IMultiImplementRegister extends IProvider {
  void add(Class<?> keyClass, RouteMeta routeMeta);
}
