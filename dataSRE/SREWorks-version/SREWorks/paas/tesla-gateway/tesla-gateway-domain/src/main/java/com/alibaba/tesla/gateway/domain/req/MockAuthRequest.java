package com.alibaba.tesla.gateway.domain.req;

import lombok.Data;

import java.io.Serializable;

/**
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class MockAuthRequest implements Serializable {

    private String requestUri;

    private Object matchedRule;

}
