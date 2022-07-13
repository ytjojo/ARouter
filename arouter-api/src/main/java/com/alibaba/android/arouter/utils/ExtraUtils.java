package com.alibaba.android.arouter.utils;

import android.app.Activity;
import android.os.Bundle;

public class ExtraUtils {
  public static boolean containsKey(Activity activity, String key) {
    return (activity.getIntent().getExtras() != null && activity.getIntent().getExtras().containsKey(key));
  }
  
  public static boolean containsKey(Bundle bundle, String key) {
    return (bundle != null && bundle.containsKey(key));
  }
}
