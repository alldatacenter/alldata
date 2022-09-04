package com.alibaba.sreworks.common.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.alibaba.fastjson.JSONObject;

public class FileUtil {

    public static String read(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));

        String data;
        while ((data = br.readLine()) != null) {
            sb.append(data);
        }
        br.close();
        return sb.toString();
    }

    public static <T> T readObject(String filePath, Class<T> clazz) throws IOException {
        String content = read(filePath);
        return JSONObject.parseObject(content, clazz);
    }

}
