package com.alibaba.android.arouter.demo.module1.testtemplate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.SparseArray;

import com.alibaba.android.arouter.demo.service.model.TestObj;
import com.alibaba.android.arouter.demo.service.model.TestParcelable;
import com.alibaba.android.arouter.demo.service.model.TestSerializable;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Action;
import com.alibaba.android.arouter.facade.annotation.Query;
import com.alibaba.android.arouter.facade.annotation.RequestCode;
import com.alibaba.android.arouter.facade.annotation.TargetPath;
import com.alibaba.android.arouter.facade.callback.NavCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ITestNavigator {


    @Action("testactivity")
    @RequestCode(300)
    @TargetPath("/test/activity1")
    Postcard navigateTest(Activity activity, @RequestCode int requestCode, @Query("map") Map<String, List<TestObj>> map, Uri uri);

    @TargetPath("/test/activity2key")
    Intent navigateTest2(Activity activity,@Query("key1") String mykey);

    @Action("testactivity")
    @RequestCode(300)
    @TargetPath("/test/activity1")
    void navigateTest(Activity activity, @RequestCode int requestCode, @Query("age") int age,@Query("name") String userName, int height, boolean boy, NavCallback navCallback);




    @Action("testactivity")
    @TargetPath("/test/activity1")
    void navigateTest(Activity activity, String name, Integer height, long high,@Query("boy") Boolean girl, Byte byteFlag, short shortFlag, @Query("age") int age, char ch, float fl, double dou, TestSerializable ser, TestParcelable pac, CharSequence charSequence, byte[] bytes, CharSequence[] charSequenceArray, char[] charArray, short[] sh, float[] floats, SparseArray<TestParcelable> sparseArrays, ArrayList<Integer> integerArrayList, ArrayList<CharSequence> charSequenceArrayList, ArrayList<String> shortArray, ArrayList<TestParcelable> parcelables, Map<String, List<TestObj>> map);

    public interface ItestStaticMethod{
        @TargetPath("/test/getmap")
        Map<String, List<TestObj>> getMap();
    }
}