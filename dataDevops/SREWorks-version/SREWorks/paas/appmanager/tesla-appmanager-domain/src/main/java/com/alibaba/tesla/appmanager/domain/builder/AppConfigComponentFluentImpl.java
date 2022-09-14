package com.alibaba.tesla.appmanager.domain.builder;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;

import java.util.ArrayList;
import java.util.List;

public class AppConfigComponentFluentImpl<A extends AppConfigComponentFluent<A>>
        extends BaseFluent<A>
        implements AppConfigComponentFluent<A> {

    private String revisionName;
    private List<AppConfigParameterValueBuilder> parameterValues;
    private List<AppConfigComponentScopeBuilder> scopes;

    @Override
    public String getRevisionName() {
        return this.revisionName;
    }

    @Override
    public ComponentTypeEnum getComponentType() {
        return DeployAppRevisionName.valueOf(this.revisionName).getComponentType();
    }

    @Override
    public String getComponentName() {
        return DeployAppRevisionName.valueOf(this.revisionName).getComponentName();
    }

    @Override
    public String getPackageVersion() {
        return DeployAppRevisionName.valueOf(this.revisionName).getVersion();
    }

    @Override
    public A withRevisionName(String revisionName) {
        this.revisionName = revisionName;
        return (A) this;
    }

    @Override
    public A withRevisionName(ComponentTypeEnum componentType, String componentName, String packageVersion) {
        this.revisionName = String.format("%s|%s|%s", componentType.toString(), componentName, packageVersion);
        return (A) this;
    }

    @Override
    public List<AppConfigParameterValue> getParameterValues() {
        return parameterValues == null ? new ArrayList<>() : build(parameterValues);
    }

    @Override
    public A withParameterValues(List<AppConfigParameterValue> parameterValues) {
        if (parameterValues != null) {
            this.parameterValues = new ArrayList<>();
            for (AppConfigParameterValue item : parameterValues) {
                this.addToParameterValues(item);
            }
        } else {
            this.parameterValues = null;
        }
        return (A) this;
    }

    @Override
    public A addToParameterValues(AppConfigParameterValue... parameterValues) {
        if (this.parameterValues == null) {
            this.parameterValues = new ArrayList<>();
        }
        for (AppConfigParameterValue item : parameterValues) {
            this.parameterValues.add(new AppConfigParameterValueBuilder(item));
        }
        return (A) this;
    }

    @Override
    public List<AppConfigComponentScope> getScopes() {
        return scopes == null ? new ArrayList<>() : build(scopes);
    }

    @Override
    public A withScopes(List<AppConfigComponentScope> scopes) {
        if (scopes != null) {
            this.scopes = new ArrayList<>();
            for (AppConfigComponentScope item : scopes) {
                this.addToScopes(item);
            }
        } else {
            this.scopes = null;
        }
        return (A) this;
    }

    @Override
    public A addToScopes(AppConfigComponentScope... scopes) {
        if (this.scopes == null) {
            this.scopes = new ArrayList<>();
        }
        for (AppConfigComponentScope item : scopes) {
            this.scopes.add(new AppConfigComponentScopeBuilder(item));
        }
        return (A) this;
    }
}
