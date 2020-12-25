package com.alibaba.android.arouter.demo.module1.testactivity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.module1.R;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import java.util.ArrayList;

@Route(path = "/test/activity4", priority = 100, secondaryPathes = {"/test/home/pro<name>/<extra>/<id>"})
public class Test4Activity extends AppCompatActivity {

    @Autowired
    public String extra;

    @Autowired
    public long id;

    @Autowired
    public String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);
        ARouter.getInstance().inject(this);
        ArrayList<String> keys = getIntent().getExtras().getStringArrayList(ARouter.AUTO_INJECT_PLACEHOLDERS);
        ((TextView) findViewById(R.id.test)).setText("I am " + Test4Activity.class.getName());
        String extra = getIntent().getStringExtra("extra");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("extra = ");
        stringBuilder.append(this.extra);
        stringBuilder.append(" name = ");
        stringBuilder.append(this.name);
        stringBuilder.append("  id = ");
        stringBuilder.append(this.id);
        for(String key : keys){
            stringBuilder.append("\n key  =" +key );
        }
        if (!TextUtils.isEmpty(extra)) {
            ((TextView) findViewById(R.id.test2)).setText(stringBuilder.toString());
        }
    }
}
