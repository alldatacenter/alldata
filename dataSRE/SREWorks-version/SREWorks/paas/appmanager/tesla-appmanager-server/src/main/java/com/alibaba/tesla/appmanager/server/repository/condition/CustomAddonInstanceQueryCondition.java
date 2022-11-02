package com.alibaba.tesla.appmanager.server.repository.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * @ClassName: CustomAddonInstanceQueryCondition
 * @Author: dyj
 * @DATE: 2020-12-23
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAddonInstanceQueryCondition implements Serializable {
    private static final long serialVersionUID = -6173813305036610127L;

    private String namespaceId;

    private String addonId;

    private String addonVersion;

    private String addonName;

    private Map<String, String> addonAttrs;

    private String addonInstanceId;
}
