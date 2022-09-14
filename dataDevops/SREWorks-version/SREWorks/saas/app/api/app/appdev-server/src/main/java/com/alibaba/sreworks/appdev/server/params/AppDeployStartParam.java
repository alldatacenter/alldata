package com.alibaba.sreworks.appdev.server.params;

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DO.AppPackage;
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
public class AppDeployStartParam {

    private String name;

    private Long teamId;

    private Long clusterId;

    private Long appPackageId;

    private String stageId;

    private String description;

    private List<Long> clusterResourceIdList;

    private String detailYaml;

    private String appComponentInstanceDetailMapYaml;

    private AppInstanceDetail detail() throws JsonProcessingException {
        AppInstanceDetail appInstanceDetail = YamlUtil.toObject(detailYaml, AppInstanceDetail.class);
        appInstanceDetail.setClusterResourceIdList(clusterResourceIdList);
        return appInstanceDetail;
    }

    private AppComponentInstanceDetail appComponentInstanceDetail(String name) {
        try {
            return YamlUtil.toJsonObject(appComponentInstanceDetailMapYaml)
                .getObject(name, AppComponentInstanceDetail.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AppInstance toAppInstance(AppPackage appPackage, String operator) throws JsonProcessingException {
        return AppInstance.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .name(name)
            .teamId(teamId)
            .clusterId(clusterId)
            .appId(appPackage.getAppId())
            .stageId(stageId)
            .appPackageId(appPackageId)
            .description(description)
            .detail(JSONObject.toJSONString(detail()))
            .build();

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
                .type(appComponent.getType())
                .appComponentId(appComponent.getId())
                .name(appComponent.getName())
                .detail(JSONObject.toJSONString(appComponentInstanceDetail(appComponent.getName())))
                .build())
            .collect(Collectors.toList());
    }
}
