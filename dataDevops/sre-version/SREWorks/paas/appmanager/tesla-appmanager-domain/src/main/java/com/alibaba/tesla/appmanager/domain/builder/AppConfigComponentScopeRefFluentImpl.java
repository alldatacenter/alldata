package com.alibaba.tesla.appmanager.domain.builder;

import com.alibaba.fastjson.JSONObject;

public class AppConfigComponentScopeRefFluentImpl<A extends AppConfigComponentScopeRefFluent<A>>
        extends BaseFluent<A>
        implements AppConfigComponentScopeRefFluent<A> {

    private String apiVersion;
    private String kind;
    private String name;
    private JSONObject spec;

    @Override
    public String getApiVersion() {
        return this.apiVersion;
    }

    @Override
    public A withApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return (A) this;
    }

    @Override
    public String getKind() {
        return this.kind;
    }

    @Override
    public A withKind(String kind) {
        this.kind = kind;
        return (A) this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public A withName(String name) {
        this.name = name;
        return (A) this;
    }

    @Override
    public JSONObject getSpec() {
        return this.spec;
    }

    @Override
    public A withSpec(JSONObject spec) {
        this.spec = spec;
        return (A) this;
    }
}
