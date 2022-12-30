package com.alibaba.tesla.appmanager.spring.util;

import com.hubspot.jinjava.Jinjava;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Fixture 文件工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class FixtureUtil {

    /**
     * 获取指定 fixture 文件，并根据参数进行模板渲染
     *
     * @param path       相对于 fixtures 文件夹的相对路径
     * @param parameters 渲染参数字典
     * @return 渲染后的文件内容
     */
    public static String getFixture(String path, Map<String, Object> parameters) throws IOException {
        String content = getFixture(path);
        if (parameters == null || parameters.size() == 0) {
            return content;
        }

        Jinjava jinjava = new Jinjava();
        return jinjava.render(content, parameters);
    }

    /**
     * 获取指定 fixture 文件内容
     *
     * @param path       相对于 fixtures 文件夹的相对路径
     * @return 渲染后的文件内容
     */
    public static String getFixture(String path) throws IOException {
        Resource resource = new ClassPathResource("fixtures/" + path);
        String resourcePath = resource.getFile().getAbsolutePath();
        return new String(Files.readAllBytes(Paths.get(resourcePath)), StandardCharsets.UTF_8);
    }
}
