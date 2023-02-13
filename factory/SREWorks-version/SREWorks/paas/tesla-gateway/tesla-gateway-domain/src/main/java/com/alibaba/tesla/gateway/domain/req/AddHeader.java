package com.alibaba.tesla.gateway.domain.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 网关路由时追加的header
 * @author tandong
 * @Description:TODO
 * @date 2019/4/9 16:39
 */
@Data
public class AddHeader implements Serializable {
    private static final long serialVersionUID = -7869441323043771882L;

    private String name;

    private String value;
}
