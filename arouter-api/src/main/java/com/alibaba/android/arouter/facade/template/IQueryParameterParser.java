package com.alibaba.android.arouter.facade.template;

import android.net.Uri;
import com.alibaba.android.arouter.facade.Postcard;
import java.util.HashMap;

public interface IQueryParameterParser {
  HashMap<String, String> parse(Uri uri, Postcard postcard);
}
