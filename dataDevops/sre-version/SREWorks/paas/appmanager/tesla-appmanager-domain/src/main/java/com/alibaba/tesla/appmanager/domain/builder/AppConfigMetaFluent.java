package com.alibaba.tesla.appmanager.domain.builder;

public interface AppConfigMetaFluent<A extends AppConfigMetaFluent<A>> extends Fluent<A> {

    String getAppId();

    A withAppId(String appId);

    String getClusterId();

    A withClusterId(String clusterId);

    String getNamespaceId();

    A withNamespaceId(String namespaceId);

    String getStageId();

    A withStageId(String stageId);

    Long getAppPackageId();

    A withAppPackageId(Long appPackageId);
}
