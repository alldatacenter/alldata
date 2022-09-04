package com.alibaba.tesla.gateway.server.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class Filter implements Serializable {
    private String name;
    private String type;
    private boolean debug;
    private boolean enable;
    private Object config;
}
