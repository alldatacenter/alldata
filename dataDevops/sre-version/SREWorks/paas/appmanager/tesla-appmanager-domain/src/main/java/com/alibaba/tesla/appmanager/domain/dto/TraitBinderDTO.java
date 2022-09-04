package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;

import lombok.Data;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/11/10.
 */
@Data
public class TraitBinderDTO {
    /**
     * 插件ID
     */
    private String name;

    /**
     *
     */
    private JSONObject spec;
}
