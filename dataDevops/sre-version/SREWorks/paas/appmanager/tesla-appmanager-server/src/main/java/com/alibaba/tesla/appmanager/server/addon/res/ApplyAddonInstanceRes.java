package com.alibaba.tesla.appmanager.server.addon.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyAddonInstanceRes implements Serializable {

    private static final long serialVersionUID = -4676282674836747297L;

    /**
     * 描述该资源是否准备好
     * if true，addonInstanceId is available
     * if false, addonInstanceTaskId is available
     */
    private boolean ready;

    /**
     * Addon Instance ID
     */
    private String addonInstanceId;

    /**
     * Addon Instance Task ID
     */
    private long addonInstanceTaskId;
}
