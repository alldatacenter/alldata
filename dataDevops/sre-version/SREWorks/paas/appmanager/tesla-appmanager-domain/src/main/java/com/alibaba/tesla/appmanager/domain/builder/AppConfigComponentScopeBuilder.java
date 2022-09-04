package com.alibaba.tesla.appmanager.domain.builder;

public class AppConfigComponentScopeBuilder
        extends AppConfigComponentScopeFluentImpl<AppConfigComponentScopeBuilder>
        implements Builder<AppConfigComponentScope> {

    private AppConfigComponentScopeFluent<?> fluent;

    public AppConfigComponentScopeBuilder() {
        this(new AppConfigComponentScope());
    }

    public AppConfigComponentScopeBuilder(AppConfigComponentScopeFluent<?> fluent) {
        this(fluent, new AppConfigComponentScope());
    }

    public AppConfigComponentScopeBuilder(AppConfigComponentScope instance) {
        this.fluent = this;
        this.fluent.withScopeRef(instance.getScopeRef());
    }

    public AppConfigComponentScopeBuilder(AppConfigComponentScopeFluent<?> fluent, AppConfigComponentScope instance) {
        this.fluent = fluent;
        this.fluent.withScopeRef(instance.getScopeRef());
    }

    @Override
    public AppConfigComponentScope build() {
        return AppConfigComponentScope.builder()
                .scopeRef(this.fluent.getScopeRef())
                .build();
    }
}
