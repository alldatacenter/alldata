package com.platform.app.http.okhttp.builder;

import com.platform.app.http.okhttp.request.PostFileRequest;
import com.platform.app.http.okhttp.request.RequestCall;

import java.io.File;

import okhttp3.MediaType;

/**
 * Created by wulinhao on 2019/9/14.
 */
public class PostFileBuilder extends OkHttpRequestBuilder<PostFileBuilder>
{
    private File file;
    private MediaType mediaType;


    public OkHttpRequestBuilder file(File file)
    {
        this.file = file;
        return this;
    }

    public OkHttpRequestBuilder mediaType(MediaType mediaType)
    {
        this.mediaType = mediaType;
        return this;
    }

    @Override
    public RequestCall build()
    {
        return new PostFileRequest(url, tag, params, headers, file, mediaType,id).build();
    }


}
