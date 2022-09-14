package com.alibaba.tesla.appmanager.domain.builder;

public class AppConfigBuilder extends AppConfigFluentImpl<AppConfigBuilder> implements Builder<AppConfig> {

    private AppConfigFluent<?> fluent;

    public AppConfigBuilder() {
        this(new AppConfig());
    }

    public AppConfigBuilder(AppConfig instance) {
        this.fluent = this;
        this.fluent.withMetadata(instance.getMetadata());
        this.fluent.withComponents(instance.getComponents());
    }

    public AppConfigBuilder(AppConfigFluent<?> fluent) {
        this.fluent = fluent;
        this.fluent.withMetadata(fluent.getMetadata());
        this.fluent.withComponents(fluent.getComponents());
    }

    @Override
    public AppConfig build() {
        return AppConfig.builder()
                .metadata(this.fluent.getMetadata())
                .components(this.fluent.getComponents())
                .build();
    }
}
