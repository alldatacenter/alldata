package com.alibaba.sreworks.appdev.server.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DTO.AppComponentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppComponentCreateByHelmParam {

    private String name;

    private String description;

    private String chartUrl;

    private String repoUrl;

    private String chartName;

    private String chartVersion;

    public AppComponent toAppComponent(Long appId, String operator) {
        return AppComponent.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .appId(appId)
            .type(AppComponentType.HELM.name())
            .name(getName())
            .typeDetail(JSONObject.toJSONString(JsonUtil.map(
                "chartUrl", chartUrl,
                "repoUrl", repoUrl,
                "chartName", chartName,
                "chartVersion", chartVersion
            )))
            .detail("")
            .description(getDescription())
            .build();
    }

}
