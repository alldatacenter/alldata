package com.alibaba.tesla.appmanager.domain.req.groovy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Git Clone 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroovyUpgradeReq implements Serializable {

    /**
     * 种类
     */
    private String kind;

    /**
     * 名称
     */
    private String name;

    /**
     * Groovy 代码
     */
    private String code;
}
