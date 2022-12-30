package com.alibaba.sreworks.job.taskhandler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
public class ApiContent {

    private String url;

    private ApiContentMethod method;

    private Map<String, String> headers;

    private Map<String, String> varConfMap;

    private String contentType;

    private String body;

    public Map<String, String> headers() {
        return headers == null ? new HashMap<>() : headers;
    }

}
