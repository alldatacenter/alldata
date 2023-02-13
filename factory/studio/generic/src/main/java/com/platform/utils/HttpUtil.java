package com.platform.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HttpUtil {

    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";

    public static String getBodyString(HttpServletRequest request) {
        String method = request.getMethod();
        String bodyString;
        if (METHOD_GET.equals(method)) {
            bodyString = doGet(request);
        } else if (METHOD_POST.equals(method)) {
            bodyString = doPost(request);
        } else {
            // 其他请求方式暂不处理
            return null;
        }
        return bodyString;
    }

    private static String doPost(HttpServletRequest request) {
        StringBuffer sb = new StringBuffer();
        InputStream inputStream;
        BufferedReader bufferedReader;
        try {
            //将数据保存到数组中，每次读取的时候，都读取一遍
            inputStream = request.getInputStream();
            //将字节数组当做输出的目的地
            //字节流转换为字符流（处理流）
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            log.error("数据读取异常", e);
        }
        return sb.toString();
    }

    private static String doGet(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String nextElement = parameterNames.nextElement();
            String parameter = request.getParameter(nextElement);
            map.put(nextElement, parameter);
        }
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (JsonProcessingException e) {
        }
        return null;
    }
}
