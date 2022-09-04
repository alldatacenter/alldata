package com.alibaba.tesla.appmanager.domain.builder;

public interface AppConfigComponentScopeFluent<A extends AppConfigComponentScopeFluent<A>> extends Fluent<A> {

    AppConfigComponentScopeRef getScopeRef();

    A withScopeRef(AppConfigComponentScopeRef scopeRef);
}
