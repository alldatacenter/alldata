package com.alibaba.tesla.appmanager.domain.builder;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;

import java.util.List;

public interface AppConfigComponentFluent<A extends AppConfigComponentFluent<A>> extends Fluent<A> {

    String getRevisionName();

    ComponentTypeEnum getComponentType();

    String getComponentName();

    String getPackageVersion();

    A withRevisionName(String revisionName);

    A withRevisionName(ComponentTypeEnum componentType, String componentName, String packageVersion);

    List<AppConfigParameterValue> getParameterValues();

    A withParameterValues(List<AppConfigParameterValue> parameterValues);

    A addToParameterValues(AppConfigParameterValue... parameterValues);

    List<AppConfigComponentScope> getScopes();

    A withScopes(List<AppConfigComponentScope> scopes);

    A addToScopes(AppConfigComponentScope... scopes);
}
