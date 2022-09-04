package com.alibaba.tesla.gateway.server.util;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaHostUtilTest {

    @Test
    public void inTeslaHost() {

        String url = "https://tesladaily.alibaba-inc.com/gateway/doc.html";
        boolean res = TeslaHostUtil.inTeslaHost(url);
        System.out.println(res);
    }

    @Test
    public void testRawQuery() throws URISyntaxException {
        URI uri = new URI("http://gateway.tesla.alibaba-inc.com/service/teslacmdb/entity/product/select?product=odps");
        System.out.println(JSONObject.toJSONString(uri));
    }
}