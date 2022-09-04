package com.alibaba.tesla.appmanager.server.addon.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @ClassName: ApplyCustomAddonInstanceRes
 * @Author: dyj
 * @DATE: 2020-12-23
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCustomAddonInstanceRes implements Serializable {
    private static final long serialVersionUID = 8950130811146140062L;

    /**
     * 描述该资源是否准备好
     * if true，addonInstanceId is available
     * if false, addonInstanceTaskId is available
     */
    private boolean ready;

    /**
     * Custom Addon Instance ID
     */
    private String customAddonInstanceId;

    /**
     * Custom Addon Instance Task ID
     */
    private long customAddonInstanceTaskId;
}
