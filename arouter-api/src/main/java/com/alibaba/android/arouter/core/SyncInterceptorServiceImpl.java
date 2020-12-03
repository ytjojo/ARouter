package com.alibaba.android.arouter.core;

import android.content.Context;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.service.SyncInterceptorService;
import com.alibaba.android.arouter.facade.template.ISyncInterceptor;
import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.utils.CollectionUtils;
import java.util.Iterator;
import java.util.List;

@Route(path = "/arouter/service/syncinterceptor")
public class SyncInterceptorServiceImpl implements SyncInterceptorService {
  public boolean doInterceptions(Context paramContext, Postcard paramPostcard) {
    List list = ARouter.getInstance().getMultiImplements(ISyncInterceptor.class);
    if (!CollectionUtils.isEmpty(list)) {
      Iterator<ISyncInterceptor> iterator = list.iterator();
      while (iterator.hasNext()) {
        if (((ISyncInterceptor)iterator.next()).process(paramContext, paramPostcard))
          return true; 
      } 
    } 
    return false;
  }
  
  public void init(Context paramContext) {}
}
