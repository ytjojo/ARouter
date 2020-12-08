package com.alibaba.android.arouter.demo.module1.testmethodinvoker;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import com.alibaba.android.arouter.facade.annotation.Route;

public class MyDialog extends Dialog {
  @Route(path = "/test/dialog")
  public MyDialog(@NonNull Context paramContext, Uri paramUri) {
    super(paramContext);
  }
}