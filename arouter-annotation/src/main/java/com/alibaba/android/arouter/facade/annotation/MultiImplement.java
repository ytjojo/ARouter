package com.alibaba.android.arouter.facade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface MultiImplement {
  int priority() default -1;
  
  Class value();
}