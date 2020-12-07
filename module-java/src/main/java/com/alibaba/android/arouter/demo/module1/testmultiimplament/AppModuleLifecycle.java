package com.alibaba.android.arouter.demo.module1.testmultiimplament;

import com.alibaba.android.arouter.facade.annotation.MultiImplement;

@MultiImplement(priority = 1, value = IModuleLifecycle.class)
public class AppModuleLifecycle implements IModuleLifecycle {
  public int getPrioriry() {
    return 1;
  }
}
