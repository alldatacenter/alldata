package com.alibaba.tesla.appmanager.domain.builder;

public class AppConfigMetaBuilder extends AppConfigMetaFluentImpl<AppConfigMetaBuilder>
        implements Builder<AppConfigMeta> {

    private AppConfigMetaFluent<?> fluent;

    public AppConfigMetaBuilder() {
        this(new AppConfigMeta());
    }

    public AppConfigMetaBuilder(AppConfigMetaFluent<?> fluent) {
        this(fluent, new AppConfigMeta());
    }

    public AppConfigMetaBuilder(AppConfigMeta instance) {
        this.fluent = this;
        this.fluent.withAppId(instance.getAppId());
        this.fluent.withClusterId(instance.getClusterId());
        this.fluent.withNamespaceId(instance.getNamespaceId());
        this.fluent.withStageId(instance.getStageId());
        this.fluent.withAppPackageId(instance.getAppPackageId());
    }

    public AppConfigMetaBuilder(AppConfigMetaFluent<?> fluent, AppConfigMeta instance) {
        this.fluent = fluent;
        this.fluent.withAppId(instance.getAppId());
        this.fluent.withClusterId(instance.getClusterId());
        this.fluent.withNamespaceId(instance.getNamespaceId());
        this.fluent.withStageId(instance.getStageId());
        this.fluent.withAppPackageId(instance.getAppPackageId());
    }

    public AppConfigMeta build() {
        return AppConfigMeta.builder()
                .appId(this.fluent.getAppId())
                .clusterId(this.fluent.getClusterId())
                .namespaceId(this.fluent.getNamespaceId())
                .stageId(this.fluent.getStageId())
                .appPackageId(this.fluent.getAppPackageId())
                .build();
    }
}
