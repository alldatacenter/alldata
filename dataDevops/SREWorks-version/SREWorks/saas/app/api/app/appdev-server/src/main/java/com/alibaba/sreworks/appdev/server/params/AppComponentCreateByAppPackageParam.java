package com.alibaba.sreworks.appdev.server.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DTO.AppComponentAppPackageDetail;
import com.alibaba.sreworks.domain.DTO.AppComponentType;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppComponentCreateByAppPackageParam {

    private String name;

    private String description;

    private String detailYaml;

    private Long appPackageId;

    public AppComponent toAppComponent(Long appId, String operator) throws JsonProcessingException {
        return AppComponent.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .appId(appId)
            .type(AppComponentType.APP_PACKAGE.name())
            .name(getName())
            .typeDetail(JSONObject.toJSONString(JsonUtil.map(
                "id", appPackageId
            )))
            .detail(YamlUtil.toJson(detailYaml))
            .description(getDescription())
            .build();
    }

}
