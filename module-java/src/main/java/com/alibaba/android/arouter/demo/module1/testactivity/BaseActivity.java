package com.alibaba.android.arouter.demo.module1.testactivity;

import androidx.appcompat.app.AppCompatActivity;


import com.alibaba.android.arouter.facade.annotation.Autowired;

/**
 * Base Activity (Used for test inject)
 */
public class BaseActivity extends AppCompatActivity {
    @Autowired(desc = "姓名")
    String name = "jack";
}
