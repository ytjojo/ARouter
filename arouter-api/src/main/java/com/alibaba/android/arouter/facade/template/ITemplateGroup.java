package com.alibaba.android.arouter.facade.template;

import java.util.Map;

public interface ITemplateGroup {
  void loadInto(Map<Class, Class> templates);
}
