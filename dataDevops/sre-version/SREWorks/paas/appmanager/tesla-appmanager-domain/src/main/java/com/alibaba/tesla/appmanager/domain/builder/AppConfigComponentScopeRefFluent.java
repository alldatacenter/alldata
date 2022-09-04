package com.alibaba.tesla.appmanager.domain.builder;

import com.alibaba.fastjson.JSONObject;

public interface AppConfigComponentScopeRefFluent<A extends AppConfigComponentScopeRefFluent<A>> extends Fluent<A> {

    String getApiVersion();

    A withApiVersion(String apiVersion);

    String getKind();

    A withKind(String kind);

    String getName();

    A withName(String name);

    JSONObject getSpec();

    A withSpec(JSONObject spec);
}
