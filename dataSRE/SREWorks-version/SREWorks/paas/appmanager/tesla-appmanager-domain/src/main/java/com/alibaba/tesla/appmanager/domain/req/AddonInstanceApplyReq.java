package com.alibaba.tesla.appmanager.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonInstanceApplyReq implements Serializable {

    private static final long serialVersionUID = 6394724498707837399L;

    /**
     * namespace
     */
    @NotBlank
    private String namespaceId;

    /**
     * 环境标识
     */
    @NotBlank
    private String envId;

    /**
     * app id
     */
    @NotBlank
    private String appId;

    /**
     * component type
     */
    @NotBlank
    private String componentType;

    /**
     * component name
     */
    @NotBlank
    private String componentName;

    private String addonResourceYml;
}
