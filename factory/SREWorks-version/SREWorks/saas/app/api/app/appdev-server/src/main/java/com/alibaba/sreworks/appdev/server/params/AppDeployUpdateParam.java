package com.alibaba.sreworks.appdev.server.params;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DTO.AppComponentInstanceDetail;
import com.alibaba.sreworks.domain.DTO.AppInstanceDetail;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDeployUpdateParam {

    private Long appPackageId;

    private String description;

    private String detailYaml;

    private String appComponentInstanceDetailMapYaml;

    private AppComponentInstanceDetail appComponentInstanceDetail(String name) {
        try {
            return YamlUtil.toJsonObject(appComponentInstanceDetailMapYaml)
                .getObject(name, AppComponentInstanceDetail.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void patchAppInstance(AppInstance appInstance, String operator) throws JsonProcessingException {

        appInstance.setGmtModified(System.currentTimeMillis() / 1000);
        appInstance.setLastModifier(operator);
        appInstance.setAppPackageId(appPackageId);
        appInstance.setDescription(description);
        appInstance.setDetail(YamlUtil.toJson(detailYaml));

    }

    public List<AppComponentInstance> toAppComponentInstanceList(
        AppInstance appInstance, List<AppComponent> appComponents, String operator) {
        return appComponents.stream()
            .map(appComponent -> AppComponentInstance.builder()
                .gmtCreate(System.currentTimeMillis() / 1000)
                .gmtModified(System.currentTimeMillis() / 1000)
                .creator(operator)
                .lastModifier(operator)
                .appInstanceId(appInstance.getId())
                .appComponentId(appComponent.getId())
                .name(appComponent.getName())
                .detail(JSONObject.toJSONString(appComponentInstanceDetail(appComponent.getName())))
                .build())
            .collect(Collectors.toList());
    }

}
