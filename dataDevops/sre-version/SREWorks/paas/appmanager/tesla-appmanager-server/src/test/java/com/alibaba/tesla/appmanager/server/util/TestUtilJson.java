package com.alibaba.tesla.appmanager.server.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * 测试 JSON
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class TestUtilJson {

    static {
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.IgnoreErrorGetter.getMask();
    }

    /**
     * 测试 GenericResource 序列化，不出错即可
     */
    @Test
    public void testFastjsonGenericResource() {
        GenericKubernetesResource resource = new GenericKubernetesResource();
        log.info(JSONObject.toJSONString(resource));
    }
}
