package com.alibaba.tesla.appmanager.domain.builder;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigComponentScopeRef {

    private String apiVersion;

    private String kind;

    private String name;

    private JSONObject spec;
}
