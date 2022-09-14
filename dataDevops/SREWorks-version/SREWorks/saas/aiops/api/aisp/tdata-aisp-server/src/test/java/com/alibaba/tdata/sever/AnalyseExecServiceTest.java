package com.alibaba.tdata.sever;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.alibaba.fastjson.JSONObject;

import com.hubspot.jinjava.Jinjava;
import io.micrometer.core.instrument.util.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @ClassName: AnalyseExecServiceTest
 * @Author: dyj
 * @DATE: 2021-12-13
 * @Description:
 **/
public class AnalyseExecServiceTest {

    @Test
    public void codeTest() throws IOException {
        String s = loadClassPathFile("code/python.tpl");
        JSONObject parameters = new JSONObject();
        JSONObject input = new JSONObject();
        input.put("taskType", "sync");
        parameters.put("input", JSONObject.toJSONString(input.toJSONString()));
        parameters.put("sceneCode", "bentomlScene");
        parameters.put("detectorCode", "bentoml_test");
        System.out.println(jinjaRender(s, parameters));
    }



    private String loadClassPathFile(String filePath) throws IOException {
        Resource config = new ClassPathResource(filePath);
        InputStream inputStream = config.getInputStream();
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    private String jinjaRender(String raw, JSONObject parameters) {
        Jinjava jinjava = new Jinjava();
        return jinjava.render(raw, parameters);
    }
}
