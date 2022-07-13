package com.alibaba.android.arouter.demo.module1.testmultiimplament;

import com.alibaba.android.arouter.facade.annotation.MultiImplement;

@MultiImplement(priority = 4, value = IModuleLifecycle.class)
public class MainModuleLifecycle implements IModuleLifecycle {
  public int getPrioriry() {
    return 4;
  }

  @Override
  public void onCreate() {

  }
}