package com.alibaba.tesla.appmanager.domain.builder;

public class AppConfigComponentScopeFluentImpl<A extends AppConfigComponentScopeFluent<A>>
        extends BaseFluent<A>
        implements AppConfigComponentScopeFluent<A> {

    private AppConfigComponentScopeRefBuilder scopeRef;

    @Override
    public AppConfigComponentScopeRef getScopeRef() {
        return this.scopeRef == null ? null : this.scopeRef.build();
    }

    @Override
    public A withScopeRef(AppConfigComponentScopeRef scopeRef) {
        this.scopeRef = new AppConfigComponentScopeRefBuilder(scopeRef);
        return (A) this;
    }
}
