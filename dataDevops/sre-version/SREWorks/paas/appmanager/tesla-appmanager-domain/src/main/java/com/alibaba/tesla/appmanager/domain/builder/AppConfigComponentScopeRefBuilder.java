package com.alibaba.tesla.appmanager.domain.builder;

public class AppConfigComponentScopeRefBuilder
        extends AppConfigComponentScopeRefFluentImpl<AppConfigComponentScopeRefBuilder>
        implements Builder<AppConfigComponentScopeRef> {

    private AppConfigComponentScopeRefFluent<?> fluent;

    public AppConfigComponentScopeRefBuilder() {
        this(new AppConfigComponentScopeRef());
    }

    public AppConfigComponentScopeRefBuilder(AppConfigComponentScopeRefFluent<?> fluent) {
        this(fluent, new AppConfigComponentScopeRef());
    }

    public AppConfigComponentScopeRefBuilder(AppConfigComponentScopeRef instance) {
        this.fluent = this;
        this.fluent.withApiVersion(instance.getApiVersion());
        this.fluent.withKind(instance.getKind());
        this.fluent.withName(instance.getName());
        this.fluent.withSpec(instance.getSpec());
    }

    public AppConfigComponentScopeRefBuilder(
            AppConfigComponentScopeRefFluent<?> fluent, AppConfigComponentScopeRef instance) {
        this.fluent = fluent;
        this.fluent.withApiVersion(instance.getApiVersion());
        this.fluent.withKind(instance.getKind());
        this.fluent.withName(instance.getName());
        this.fluent.withSpec(instance.getSpec());
    }

    @Override
    public AppConfigComponentScopeRef build() {
        return AppConfigComponentScopeRef.builder()
                .apiVersion(this.fluent.getApiVersion())
                .kind(this.fluent.getKind())
                .name(this.fluent.getName())
                .spec(this.fluent.getSpec())
                .build();
    }
}
