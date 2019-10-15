package com.platform.app.http.okhttp.builder;

import java.util.Map;

/**
 * Created by wulinhao on 2019/9/1.
 */
public interface HasParamsable
{
    OkHttpRequestBuilder params(Map<String, String> params);
    OkHttpRequestBuilder addParams(String key, String val);
}
