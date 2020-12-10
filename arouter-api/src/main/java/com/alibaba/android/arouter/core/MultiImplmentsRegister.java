package com.alibaba.android.arouter.core;

import com.alibaba.android.arouter.base.PriorityList;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.alibaba.android.arouter.facade.template.IMultiImplementRegister;

class MultiImplmentsRegister implements IMultiImplementRegister {
    public static MultiImplmentsRegister getInstance() {
        return HOLDER.instance;
    }

    @Override
    public void add(Class<?> keyClass, RouteMeta routeMeta) {
        if (!Warehouse.multImplments.containsKey(keyClass)) {
            Warehouse.multImplments.put(keyClass, new PriorityList());
        }
        ((PriorityList) Warehouse.multImplments.get(keyClass)).addItem(routeMeta, routeMeta.getPriority());
    }

    private static class HOLDER {
        static MultiImplmentsRegister instance = new MultiImplmentsRegister();
    }
}
