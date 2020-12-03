package com.alibaba.android.arouter.utils;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.service.RouteGlobleCallbackService;
import com.alibaba.android.arouter.launcher.ARouter;

public class GlobleCallbackNotifer {
  public static void onArrival(Postcard postcard) {
    RouteGlobleCallbackService routeGlobleCallbackService = (RouteGlobleCallbackService)ARouter.getInstance().navigation(RouteGlobleCallbackService.class);
    if (routeGlobleCallbackService != null)
      routeGlobleCallbackService.onArrival(postcard); 
  }
  
  public static void onFound(Postcard postcard) {
    RouteGlobleCallbackService routeGlobleCallbackService = (RouteGlobleCallbackService)ARouter.getInstance().navigation(RouteGlobleCallbackService.class);
    if (routeGlobleCallbackService != null)
      routeGlobleCallbackService.onFound(postcard); 
  }
  
  public static void onHangUp(Postcard postcard) {
    RouteGlobleCallbackService routeGlobleCallbackService = (RouteGlobleCallbackService)ARouter.getInstance().navigation(RouteGlobleCallbackService.class);
    if (routeGlobleCallbackService != null)
      routeGlobleCallbackService.onHangUp(postcard); 
  }
  
  public static void onInterrupt(Postcard postcard) {
    RouteGlobleCallbackService routeGlobleCallbackService = (RouteGlobleCallbackService)ARouter.getInstance().navigation(RouteGlobleCallbackService.class);
    if (routeGlobleCallbackService != null)
      routeGlobleCallbackService.onInterrupt(postcard); 
  }
  
  public static void onLost(Postcard postcard) {
    RouteGlobleCallbackService routeGlobleCallbackService = (RouteGlobleCallbackService)ARouter.getInstance().navigation(RouteGlobleCallbackService.class);
    if (routeGlobleCallbackService != null)
      routeGlobleCallbackService.onLost(postcard); 
  }
}
