package com.alibaba.tesla.productops.interceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyHttpServletRequest extends HttpServletRequestWrapper {

    Map<String, String[]> params = new HashMap<>();

    public MyHttpServletRequest(HttpServletRequest request) {
        super(request);
        if (request.getParameterMap() != null) {
            params = new HashMap<>(request.getParameterMap());
        }
        if (!params.containsKey("stageId")) {
            params.put("stageId", new String[] {"prod"});
        }

    }

    @Override
    public String getParameter(String name) {
        return Arrays.toString(params.get(name));
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(params.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return params.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return params;
    }
}
