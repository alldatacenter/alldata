package com.alibaba.tesla.gateway.server.filter.global;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class LogGlobalFilterTest {

    @Test
    public void test() throws URISyntaxException {
        URI uri = new URI("http://unknown/unkonwn");
        System.out.println(uri.getHost());
        System.out.println(uri.getPath());
    }

    @Test
    public void test2() throws URISyntaxException {
        URI uri = new URI("http://gateway.tesla.alibaba-inc.com:7001/test?name=nicqiang");
        String path = uri.getRawPath();
        String query = uri.getQuery();
        System.out.println(query);
        System.out.println(path);
        System.out.println(uri.getHost());
        System.out.println(uri.getPath());
    }

}