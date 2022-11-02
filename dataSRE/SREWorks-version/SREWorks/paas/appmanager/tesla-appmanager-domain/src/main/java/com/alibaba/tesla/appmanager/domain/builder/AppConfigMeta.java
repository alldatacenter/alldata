package com.alibaba.tesla.appmanager.domain.builder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigMeta {

    private String appId;

    private String clusterId;

    private String namespaceId;

    private String stageId;

    private Long appPackageId;
}
