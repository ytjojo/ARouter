package com.alibaba.android.arouter.facade.template;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.alibaba.android.arouter.core.LogisticsCenter;
import com.alibaba.android.arouter.facade.enums.RouteType;
import com.alibaba.android.arouter.facade.model.RouteMeta;

import java.util.HashMap;
import java.util.Map;

public interface IRouteMetaRegister {
    void loadInto(Map<String, RouteMeta> atlas);
}
