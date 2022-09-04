package com.alibaba.sreworks.flyadmin.server.services;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FlyadminAuthproxyService {

    public static String getAuthProxyEndpoint()  {
        return System.getenv("AUTHPROXY_ENDPOINT");
    }

}
