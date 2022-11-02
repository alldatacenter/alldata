package com.alibaba.tesla.appmanager.domain.builder;

public class AppConfigComponentBuilder extends AppConfigComponentFluentImpl<AppConfigComponentBuilder>
        implements Builder<AppConfigComponent> {

    private AppConfigComponentFluent<?> fluent;

    public AppConfigComponentBuilder() {
        this(new AppConfigComponent());
    }

    public AppConfigComponentBuilder(AppConfigComponentFluent<?> fluent) {
        this(fluent, new AppConfigComponent());
    }

    public AppConfigComponentBuilder(AppConfigComponent instance) {
        this.fluent = this;
        this.fluent.withRevisionName(instance.getRevisionName());
        this.fluent.withParameterValues(instance.getParameterValues());
        this.fluent.withScopes(instance.getScopes());
    }

    public AppConfigComponentBuilder(
            AppConfigComponentFluent<?> fluent, AppConfigComponent instance) {
        this.fluent = fluent;
        this.fluent.withRevisionName(instance.getRevisionName());
        this.fluent.withParameterValues(instance.getParameterValues());
        this.fluent.withScopes(instance.getScopes());
    }

    @Override
    public AppConfigComponent build() {
        return AppConfigComponent.builder()
                .revisionName(this.fluent.getRevisionName())
                .parameterValues(this.fluent.getParameterValues())
                .scopes(this.fluent.getScopes())
                .build();
    }
}
