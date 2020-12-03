package com.alibaba.android.arouter.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class CollectionUtils {
  public static boolean isEmpty(Collection paramCollection) {
    return (paramCollection == null || paramCollection.isEmpty()) ? true : paramCollection.isEmpty();
  }
  
  public static boolean isEmpty(Map paramMap) {
    return (paramMap == null || paramMap.isEmpty()) ? true : paramMap.isEmpty();
  }
  
  public static boolean isEmpty(Set paramSet) {
    return (paramSet == null || paramSet.isEmpty()) ? true : paramSet.isEmpty();
  }
}
