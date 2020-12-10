package com.alibaba.android.arouter.demo.module1.testmethodinvoker;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.module1.R;
import com.alibaba.android.arouter.demo.service.model.TestObj;
import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyDialog extends Dialog {

  TextView tvContent;
  String content;
  @Route(path = "/test/dialog")
  public MyDialog(@NonNull Context paramContext, String content) {
    super(paramContext);
    this.content = content;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.my_dialog);
    tvContent = findViewById(R.id.tv_contrent);
    tvContent.setText(content);
  }

  public static class TestUtil {

    @Route(path = "/test/getmap")
    public static Map<String, List<TestObj>> getMap() {
      HashMap<String, List<TestObj>> map = new HashMap();
      ArrayList<TestObj> testObjs = new ArrayList<>();
      TestObj testObj = new TestObj();
      testObj.id = 1;
      testObj.name = "tinker";
      testObjs.add(testObj);
      map.put("tinker", testObjs);
      return map;
    }
  }
}