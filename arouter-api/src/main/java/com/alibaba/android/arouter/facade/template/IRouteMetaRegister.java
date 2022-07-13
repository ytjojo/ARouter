package com.alibaba.android.arouter.facade.template;


import com.alibaba.android.arouter.facade.model.RouteMeta;

import java.util.HashMap;
import java.util.Map;

public interface IRouteMetaRegister {
    void loadInto(Map<String, RouteMeta> atlas);
}
