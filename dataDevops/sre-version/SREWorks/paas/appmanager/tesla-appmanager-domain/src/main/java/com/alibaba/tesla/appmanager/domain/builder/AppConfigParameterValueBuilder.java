package com.alibaba.tesla.appmanager.domain.builder;

public class AppConfigParameterValueBuilder extends AppConfigParameterValueFluentImpl<AppConfigParameterValueBuilder>
        implements Builder<AppConfigParameterValue> {

    private AppConfigParameterValueFluent<?> fluent;

    public AppConfigParameterValueBuilder() {
        this(new AppConfigParameterValue());
    }

    public AppConfigParameterValueBuilder(AppConfigParameterValueFluent<?> fluent) {
        this(fluent, new AppConfigParameterValue());
    }

    public AppConfigParameterValueBuilder(AppConfigParameterValue instance) {
        this.fluent = this;
        this.fluent.withName(instance.getName());
        this.fluent.withValue(instance.getValue());
        this.fluent.withToFieldPaths(instance.getToFieldPaths());
    }

    public AppConfigParameterValueBuilder(AppConfigParameterValueFluent<?> fluent, AppConfigParameterValue instance) {
        this.fluent = fluent;
        this.fluent.withName(instance.getName());
        this.fluent.withValue(instance.getValue());
        this.fluent.withToFieldPaths(instance.getToFieldPaths());
    }

    @Override
    public AppConfigParameterValue build() {
        return AppConfigParameterValue.builder()
                .name(this.fluent.getName())
                .value(this.fluent.getValue())
                .toFieldPaths(this.fluent.getToFieldPaths())
                .build();
    }
}
