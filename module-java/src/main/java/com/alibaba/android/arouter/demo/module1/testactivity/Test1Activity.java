package com.alibaba.android.arouter.demo.module1.testactivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.module1.R;
import com.alibaba.android.arouter.demo.module1.testactivity.privateInterceptor.TestPrivateInterceptor;
import com.alibaba.android.arouter.demo.service.HelloService;
import com.alibaba.android.arouter.demo.service.model.TestObj;
import com.alibaba.android.arouter.demo.service.model.TestParcelable;
import com.alibaba.android.arouter.demo.service.model.TestSerializable;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Query;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.service.SerializationService;
import com.alibaba.android.arouter.launcher.ARouter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * https://m.aliyun.com/test/activity1?name=老王&age=23&boy=true&high=180
 */
@Route(path = "/test/activity1", interceptors = {TestPrivateInterceptor.class}, name = "测试用 Activity", secondaryPathes = {"/test/activity1secondary", "/test/activity1secondary2"})
public class Test1Activity extends BaseActivity {
    @Autowired
    Integer age = 10;

    @Autowired
    int height = 175;

    @Autowired(name = "boy", required = true, alternate = {"sex"})
    boolean girl;

    @Autowired
    char ch = 'A';

    @Autowired
    float fl = 12.00f;

    @Autowired
    double dou = 12.01d;

    @Autowired
    TestSerializable ser;

    @Autowired
    TestParcelable pac;

    @Autowired
    TestObj obj;

    @Autowired
    List<TestObj> objList;

    @Autowired
    Map<String, List<TestObj>> map;

    @Autowired
    public long high;

    @Autowired
    String url;

    @Autowired
    HelloService helloService;

    @Autowired
    byte byteFlag;

    @Autowired
    byte[] bytes;


    @Autowired(alternate = {"charArray2"})
    char[] charArray;

    @Autowired
    CharSequence charSequence;

    @Autowired
    CharSequence[] charSequenceArray;

    @Autowired
    ArrayList<CharSequence> charSequenceArrayList;


    @Autowired
    float[] floats;


    @Autowired
    ArrayList<Integer> integerArrayList;


    @Autowired
    ArrayList<TestParcelable> parcelables;


    @Autowired
    short[] shortArray;

    @Autowired
    short shortFlag;

    @Autowired
    SparseArray<TestParcelable> sparseArrays;

    @Autowired
    ArrayList<String> stringArrayList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);

        ARouter.getInstance().inject(this);

        // No more getter ...
        // name = getIntent().getStringExtra("name");
        // age = getIntent().getIntExtra("age", 0);
        // girl = getIntent().getBooleanExtra("girl", false);
        // high = getIntent().getLongExtra("high", 0);
        // url = getIntent().getStringExtra("url");

        String params = String.format(
                "name=%s,\n age=%s, \n height=%s,\n girl=%s,\n high=%s,\n url=%s,\n ser=%s,\n pac=%s,\n obj=%s \n ch=%s \n fl = %s, \n dou = %s, \n objList=%s, \n map=%s",
                name,
                age,
                height,
                girl,
                high,
                url,
                ser,
                pac,
                obj,
                ch,
                fl,
                dou,
                objList,
                map
        );
        helloService.sayHello("Hello moto.");

        ((TextView) findViewById(R.id.test)).setText("I am " + Test1Activity.class.getName());
        ((TextView) findViewById(R.id.test2)).setText(params);
    }


    @Route(path = "/test/getintent")
    public static Intent getIntent(Context context,@Query("name") String name, Integer height, long high,@Query("sex") Boolean girl, Byte byteFlag, short shortFlag, @Query("age") int age, char ch, float fl, double dou, TestSerializable ser, TestParcelable pac, CharSequence charSequence, byte[] bytes, CharSequence[] charSequenceArray, char[] charArray, short[] sh, float[] floats, SparseArray<TestParcelable> sparseArrays, ArrayList<Integer> integerArrayList, ArrayList<CharSequence> charSequenceArrayList, ArrayList<String> stringArrayList, ArrayList<TestParcelable> parcelables, Map<String, List<TestObj>> map) {
        Intent intent = new Intent(context, Test1Activity.class);
        intent.putExtra("name", name);
        intent.putExtra("boy",girl);
        intent.putExtra("age", age);
        intent.putExtra("byteFlag", byteFlag);
        intent.putExtra("height", height);
        intent.putExtra("long",high);
        intent.putExtra("ch", ch);
        intent.putExtra("fl", fl);
        intent.putExtra("dou", dou);
        intent.putExtra("shortFlag", shortFlag);
        intent.putExtra("ser", (Serializable) ser);
        intent.putExtra("pac", (Parcelable)pac);
        intent.putExtra("charSequence", charSequence);
        intent.putExtra("bytes", bytes);
        intent.putExtra("charSequenceArray", charSequenceArray);
        intent.putExtra("charArray", charArray);
        intent.putExtra("shortArray", sh);
        intent.putExtra("floats", floats);
        intent.putIntegerArrayListExtra("integerArrayList", integerArrayList);
        intent.putStringArrayListExtra("stringArrayList", stringArrayList);
        intent.putCharSequenceArrayListExtra("charSequenceArrayList", charSequenceArrayList);
        intent.putParcelableArrayListExtra("parcelables", parcelables);
        intent.getExtras().putSparseParcelableArray("sparseArrays", sparseArrays);
        intent.putExtra("map", ((SerializationService)ARouter.getInstance().navigation(SerializationService.class)).object2Json(map));
        return intent;
    }

    @Route(path = "/test/getintentWithObj")
    public static Intent getIntent1(Context context, TestSerializable ser, TestParcelable pac, CharSequence charSequence, SparseArray<TestParcelable> sparseArrays, ArrayList<Integer> integerArrayList, ArrayList<TestParcelable> parcelables, Map<String, List<TestObj>> map) {
        Intent intent = new Intent(context, Test1Activity.class);
        intent.putExtra("ser", (Serializable) ser);
        intent.putExtra("pac", (Parcelable)pac);
        intent.putExtra("charSequence", charSequence);
        intent.putIntegerArrayListExtra("integerArrayList", integerArrayList);
        intent.putParcelableArrayListExtra("parcelables", parcelables);
        intent.getExtras().putSparseParcelableArray("sparseArrays", sparseArrays);
        intent.putExtra("map", ((SerializationService)ARouter.getInstance().navigation(SerializationService.class)).object2Json(map));
        return intent;
    }
}
