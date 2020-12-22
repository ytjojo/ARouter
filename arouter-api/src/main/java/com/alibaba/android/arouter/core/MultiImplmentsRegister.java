package com.alibaba.android.arouter.core;

import android.content.Context;

import com.alibaba.android.arouter.base.PriorityList;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.alibaba.android.arouter.facade.template.IMultiImplementRegister;

@Route(path = "/arouter/service/multiimplmentsregister")
public class MultiImplmentsRegister implements IMultiImplementRegister {
    @Override
    public void add(Class<?> keyClass, RouteMeta routeMeta) {
        if (!Warehouse.multImplments.containsKey(keyClass)) {
            Warehouse.multImplments.put(keyClass, new PriorityList());
        }
        ((PriorityList) Warehouse.multImplments.get(keyClass)).addItem(routeMeta, routeMeta.getPriority());
    }


    @Override
    public void init(Context context) {

    }
}
