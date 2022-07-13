package com.alibaba.android.arouter.demo.module1.testdegrade;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.MultiImplement;
import com.alibaba.android.arouter.facade.service.DegradeService;

@MultiImplement(priority = 100, value = DegradeService.class)
public class ModuleDegrade implements DegradeService {
  public void init(Context paramContext) {}
  
  public boolean onLost(final Context context, Postcard paramPostcard) {
    Log.i("DegradeService", "priority =  100");
    if (paramPostcard.getPath().startsWith("/module")) {
      (new Handler()).postDelayed(new Runnable() {
            public void run() {
              Toast.makeText(context, "module find lost", Toast.LENGTH_SHORT).show();
            }
          },2000);
      return true;
    } 
    return false;
  }
}