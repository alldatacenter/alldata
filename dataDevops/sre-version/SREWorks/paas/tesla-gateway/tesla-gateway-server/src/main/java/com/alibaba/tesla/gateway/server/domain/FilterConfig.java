package com.alibaba.tesla.gateway.server.domain;

import lombok.Data;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author tandong.td@alibaba-inc.com
 */
@Repository
@Data
public class FilterConfig {
    private String name;
    private String version;
    private String env;
    private List<Filter> filters;
}
