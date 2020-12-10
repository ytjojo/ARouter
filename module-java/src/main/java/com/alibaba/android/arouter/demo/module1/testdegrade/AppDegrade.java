package com.alibaba.android.arouter.demo.module1.testdegrade;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.MultiImplement;
import com.alibaba.android.arouter.facade.service.DegradeService;

@MultiImplement(priority = 5, value = DegradeService.class)
public class AppDegrade implements DegradeService {
  public void init(Context paramContext) {}
  
  public boolean onLost(final Context context, Postcard paramPostcard) {
    Log.i("DegradeService", "priority =  5");

    (new Handler()).postDelayed(new Runnable() {
      public void run() {
        Toast.makeText(context, "module find lost", Toast.LENGTH_SHORT).show();
      }
    },6000);
    return false;
  }
}
