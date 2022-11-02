package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 附加组件实例
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddonInstanceDO {
    private static final long serialVersionUID = 1L;
    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 资源实例ID
     */
    private String addonInstanceId;

    /**
     * 该附加组件部署到的 Namespace 标识
     */
    private String namespaceId;

    /**
     * 附加组件唯一标识
     */
    private String addonId;

    /**
     * 组件名称
     */
    private String addonName;

    /**
     * 附加组件唯一标识
     */
    private String addonVersion;

    /**
     * 附加组件属性
     */
    private String addonAttrs;

    /**
     * 附加组件扩展信息
     */
    private String addonExt;

    /**
     * 附加组件 config var 字典内容
     */
    private String dataOutput;

    /**
     * 签名
     */
    private String signature;

    /**
     * 返回申请过程消耗的时间
     *
     * @return 字符串
     */
    public String costTime() {
        if (gmtCreate == null || gmtModified == null) {
            return "unknown";
        }
        return String.format("%d", (gmtModified.getTime() - gmtCreate.getTime()) / 1000);
    }
}