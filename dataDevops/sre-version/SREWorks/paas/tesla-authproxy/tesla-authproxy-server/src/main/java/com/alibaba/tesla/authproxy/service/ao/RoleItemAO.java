package com.alibaba.tesla.authproxy.service.ao;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 角色 参数
 */
@Data
public class RoleItemAO {

    /**
     * 操作人
     */
    @ApiModelProperty(hidden = true)
    private String empId;

    /**
     * 租户 ID
     */
    @ApiModelProperty(hidden = true)
    private String tenantId;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 角色 ID
     */
    private String roleId;

    /**
     * 国际化 - 场景选项
     */
    private List<Option> options;

    @Data
    public static class Option {

        /**
         * 语言
         */
        private String locale;

        /**
         * 名称
         */
        private String name;

        /**
         * 说明
         */
        private String description;
    }
}
