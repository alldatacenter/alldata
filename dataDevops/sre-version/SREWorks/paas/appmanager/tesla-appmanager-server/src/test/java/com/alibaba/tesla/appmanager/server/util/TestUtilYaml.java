package com.alibaba.tesla.appmanager.server.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 Yaml 工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class TestUtilYaml {

    /**
     * 测试 Yaml Dict 渲染
     */
    @Test
    public void testDictRender() throws Exception {
        JSONObject content = new JSONObject();
        content.put("options", new JSONObject());
        content.getJSONObject("options").put("a", "b");
        content.getJSONObject("options").put("c", new JSONArray());
        content.getJSONObject("options").getJSONArray("c").add("z1");
        content.getJSONObject("options").getJSONArray("c").add("z2");
        content.getJSONObject("options").put("d", new JSONObject());
        content.getJSONObject("options").getJSONObject("d").put("d1", "d2");
        content.getJSONObject("options").getJSONObject("d").put("d3", "d4");
        Yaml yaml = SchemaUtil.createYaml(JSONObject.class);
        String yamlContent = yaml.dump(content);
        int indent = 6;
        String preSpace = String.join("", Collections.nCopies(indent, " "));
        String output = "\n" + Arrays.stream(yamlContent.split("\n"))
                .map(line -> preSpace + line)
                .collect(Collectors.joining("\n"));
        assertThat(output).isEqualTo("\n      options:\n" +
                "        a: b\n" +
                "        c:\n" +
                "        - z1\n" +
                "        - z2\n" +
                "        d:\n" +
                "          d1: d2\n" +
                "          d3: d4");
    }
}
