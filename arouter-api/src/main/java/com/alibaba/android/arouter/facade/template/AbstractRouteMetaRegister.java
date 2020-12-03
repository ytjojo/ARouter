package com.alibaba.android.arouter.facade.template;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.alibaba.android.arouter.core.LogisticsCenter;
import com.alibaba.android.arouter.facade.enums.RouteType;
import com.alibaba.android.arouter.facade.model.RouteMeta;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRouteMetaRegister {


    protected RouteMeta buildActivity( String path,Class<?> clazz) {
        return RouteMeta.build(RouteType.ACTIVITY, clazz, path, null, null, null, null, -1, -2147483648);
    }

    protected RouteMeta buildFragment( String path,Class<?> clazz) {
        return RouteMeta.build(RouteType.FRAGMENT, clazz, path, null, null, null, null, -1, -2147483648);
    }



   public abstract void loadInto(Map<String, RouteMeta> atlas) ;
}
