package com.platform.app.http.okhttp.builder;

import com.platform.app.http.okhttp.OkHttpUtils;
import com.platform.app.http.okhttp.request.OtherRequest;
import com.platform.app.http.okhttp.request.RequestCall;

/**
 * Created by wulinhao on 2019/9/2.
 */
public class HeadBuilder extends GetBuilder
{
    @Override
    public RequestCall build()
    {
        return new OtherRequest(null, null, OkHttpUtils.METHOD.HEAD, url, tag, params, headers,id).build();
    }
}
