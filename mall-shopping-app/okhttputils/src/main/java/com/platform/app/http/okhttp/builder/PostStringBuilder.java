package com.platform.app.http.okhttp.builder;

import com.platform.app.http.okhttp.request.PostStringRequest;
import com.platform.app.http.okhttp.request.RequestCall;

import okhttp3.MediaType;

/**
 * Created by wulinhao on 2019/9/14.
 */
public class PostStringBuilder extends OkHttpRequestBuilder<PostStringBuilder>
{
    private String content;
    private MediaType mediaType;


    public PostStringBuilder content(String content)
    {
        this.content = content;
        return this;
    }

    public PostStringBuilder mediaType(MediaType mediaType)
    {
        this.mediaType = mediaType;
        return this;
    }

    @Override
    public RequestCall build()
    {
        return new PostStringRequest(url, tag, params, headers, content, mediaType,id).build();
    }


}
