package com.alibaba.android.arouter.facade.service;

import com.alibaba.android.arouter.facade.Postcard;

public interface RouteGlobleCallbackService {
  void onArrival(Postcard postcard);
  
  void onFound(Postcard postcard);
  
  void onHangUp(Postcard postcard);
  
  void onInterrupt(Postcard postcard);
  
  void onLost(Postcard postcard);
}