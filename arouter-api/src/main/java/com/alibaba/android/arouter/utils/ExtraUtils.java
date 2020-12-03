package com.alibaba.android.arouter.utils;

import android.app.Activity;
import android.os.Bundle;

public class ExtraUtils {
  public static boolean containsKey(Activity paramActivity, String paramString) {
    return (paramActivity.getIntent().getExtras() != null && paramActivity.getIntent().getExtras().containsKey(paramString));
  }
  
  public static boolean containsKey(Bundle paramBundle, String paramString) {
    return (paramBundle != null && paramBundle.containsKey(paramString));
  }
}
