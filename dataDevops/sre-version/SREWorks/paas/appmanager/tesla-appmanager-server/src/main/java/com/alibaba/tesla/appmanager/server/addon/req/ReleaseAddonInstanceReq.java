package com.alibaba.tesla.appmanager.server.addon.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 释放 Addon Instance 请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseAddonInstanceReq implements Serializable {

    private static final long serialVersionUID = -1326995704148609265L;

    /**
     * Addon Instance ID
     */
    private String addonInstanceId;
}
