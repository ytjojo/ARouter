package com.alibaba.android.arouter.demo.module1.testdegrade;

import android.content.Context;
import android.util.Log;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.MultiImplement;
import com.alibaba.android.arouter.facade.service.DegradeService;

@MultiImplement(priority = 5, value = DegradeService.class)
public class AppDegrade implements DegradeService {
  public void init(Context paramContext) {}
  
  public boolean onLost(Context paramContext, Postcard paramPostcard) {
    Log.i("DegradeService", "priority =  5");
    return false;
  }
}
