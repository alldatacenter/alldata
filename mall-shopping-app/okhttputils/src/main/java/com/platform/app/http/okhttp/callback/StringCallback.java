package com.platform.app.http.okhttp.callback;

import java.io.IOException;

import okhttp3.Response;

/**
 * Created by wulinhao on 2019/9/14.
 */
public abstract class StringCallback extends Callback<String>
{
    @Override
    public String parseNetworkResponse(Response response, int id) throws IOException
    {
        return response.body().string();
    }
}
