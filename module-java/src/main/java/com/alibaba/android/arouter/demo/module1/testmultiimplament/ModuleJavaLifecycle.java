package com.alibaba.android.arouter.demo.module1.testmultiimplament;

import com.alibaba.android.arouter.facade.annotation.MultiImplement;

@MultiImplement(priority = 20, value = IModuleLifecycle.class)
public class ModuleJavaLifecycle implements IModuleLifecycle {
  public int getPrioriry() {
    return 20;
  }
}
