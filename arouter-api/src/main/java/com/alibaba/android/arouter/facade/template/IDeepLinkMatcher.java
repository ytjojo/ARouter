package com.alibaba.android.arouter.facade.template;

import android.net.Uri;

import com.alibaba.android.arouter.facade.DeepLinkUri;
import com.alibaba.android.arouter.facade.model.RouteMeta;

import java.util.Map;

public interface IDeepLinkMatcher {
    RouteMeta findMatch(Uri uri, Map<String, DeepLinkUri> urlMapping, Map<String, RouteMeta> routes);
}
