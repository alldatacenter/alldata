package com.alibaba.tesla.tkgone.server.common.http;

import okhttp3.MediaType;

public enum HttpMediaType {

    APPLICATION_JSON(MediaType.parse("application/json; charset=utf-8"));

    private MediaType mediaType;

    HttpMediaType(MediaType mediaType){
        this.mediaType = mediaType;
    }

    public MediaType getMediaType() {
        return this.mediaType;
    }
}
