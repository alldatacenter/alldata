package com.alibaba.tesla;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.alibaba.fastjson.JSONObject;

import static org.junit.Assert.assertTrue;

import io.micrometer.core.instrument.util.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void doc() throws IOException {
        String s = loadClassPathFile("README.md");
        String s1 = JSONObject.parseObject(JSONObject.toJSONString(s), String.class);
        System.out.println(s1);
    }



    private String loadClassPathFile(String filePath) throws IOException {
        Resource config = new ClassPathResource(filePath);
        InputStream inputStream = config.getInputStream();
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
}
