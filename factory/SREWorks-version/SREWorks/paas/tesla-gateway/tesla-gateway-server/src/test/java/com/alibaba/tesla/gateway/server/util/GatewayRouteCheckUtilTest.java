package com.alibaba.tesla.gateway.server.util;

import lombok.ToString;
import org.junit.Test;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class GatewayRouteCheckUtilTest {

    @Test
    public void checkLegitimate() throws URISyntaxException {

        String url = "1232342";
        URI uri = new URI(url);
        System.out.println(uri.getHost());
    }


    @Test
    public void test2() throws URISyntaxException {
        String ttt = "http://tesla.alibaba-inc.com/tesla/platform/test?name=nicqiang";
        URI uri = new URI(ttt);
        System.out.println(uri.toString().split("\\?")[0]);
    }

    @Test
    public void tess3(){
        String path = "/a/b/c/d/e";
        String realPath = Arrays.stream(StringUtils.tokenizeToStringArray(path, "/")).skip(10)
            .collect(Collectors.joining("/"));
        System.out.println(realPath);
    }
}