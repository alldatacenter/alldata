package com.alibaba.tesla.gateway.server.locator;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class RouteDefinitionFactoryTest {

    @Test
    public void to() throws URISyntaxException {
        URI uri = new URI("http://11.165.70.5:29034/");
        System.out.println(uri.toString().endsWith("/"));
        System.out.println(uri.toString());
    }

    @Test
    public void test2() throws URISyntaxException {
        URI uri = new URI("lb://tesla-notice");
        System.out.println(uri.toString());
    }

    @Test
    public void test3() throws URISyntaxException {
        URI uri = new URI("http:");
    }

    @Test
    public void test4(){
        String url = "http://tesla.alibaba.com:7001";
        String[] split = url.split("://", 2)[1].split("/", 2);
        System.out.println(split);
    }

    @Test
    public void test5() throws URISyntaxException {
        String url = "lb://faas-tesla-faas-deploy";
        URI uri = new URI(url);
        System.out.println(uri);
    }

    @Test
    public void test6() throws URISyntaxException {
        String url = "lb://faas_tesla_fass_deploy";
        URI uri = new URI(url);
        System.out.println(uri);
    }

    @Test
    public void test7() throws URISyntaxException {
        String url = "lb://faas-tesla-faas-deploy-faas-tesla-faas-deploy-faas-tesla-faas-deploy-faas-tesla-faas-deploy";
        URI uri = new URI(url);
        System.out.println(uri);
    }

    @Test
    public void test8() throws URISyntaxException {
        String url = "http://tesla.alibaba.com:7001;http://tesla.alibaba.com:7001";
        URI uri = new URI(url);
        System.out.println(uri);
    }



}