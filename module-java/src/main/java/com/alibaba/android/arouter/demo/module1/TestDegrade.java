package com.alibaba.android.arouter.demo.module1;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.MultiImplement;
import com.alibaba.android.arouter.facade.service.DegradeService;

@MultiImplement(priority = 7, value = DegradeService.class)
public class TestDegrade implements DegradeService {
  public void init(Context paramContext) {}
  
  public boolean onLost(final Context context, Postcard paramPostcard) {
    Log.i("DegradeService", "priority =  7");
    if (paramPostcard.getPath().startsWith("/test")) {
      new Handler().postDelayed(new Runnable() {
            public void run() {
              Toast.makeText(context, "test not found", Toast.LENGTH_SHORT).show();
            }
          },4000L);
      return true;
    } 
    return false;
  }
}
