package com.alibaba.tesla.appmanager.domain.builder;

public class AppConfigMetaFluentImpl<A extends AppConfigMetaFluent<A>>
        extends BaseFluent<A>
        implements AppConfigMetaFluent<A> {

    private String appId;
    private String clusterId;
    private String namespaceId;
    private String stageId;
    private Long appPackageId;

    @Override
    public String getAppId() {
        return this.appId;
    }

    @Override
    public A withAppId(String appId) {
        this.appId = appId;
        return (A) this;
    }

    @Override
    public String getClusterId() {
        return this.clusterId;
    }

    @Override
    public A withClusterId(String clusterId) {
        this.clusterId = clusterId;
        return (A) this;
    }

    @Override
    public String getNamespaceId() {
        return this.namespaceId;
    }

    @Override
    public A withNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
        return (A) this;
    }

    @Override
    public String getStageId() {
        return this.stageId;
    }

    @Override
    public A withStageId(String stageId) {
        this.stageId = stageId;
        return (A) this;
    }

    @Override
    public Long getAppPackageId() {
        return this.appPackageId;
    }

    @Override
    public A withAppPackageId(Long appPackageId) {
        this.appPackageId = appPackageId;
        return (A) this;
    }
}
