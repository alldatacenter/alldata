package com.alibaba.sreworks.appdev.server.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DTO.AppComponentRepoDetail;
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
public class AppComponentCreateParam {

    private String name;

    private String description;

    private String detailYaml;

    private String url;

    private String branch;

    private String dockerfileTemplate;

    private Long teamRegistryId;

    private Long teamRepoId;

    public AppComponentRepoDetail repo() {
        return AppComponentRepoDetail.builder()
            .url(url)
            .branch(branch)
            .dockerfileTemplate(dockerfileTemplate)
            .build();
    }

    public AppComponent toAppComponent(Long appId, String operator) throws JsonProcessingException {
        return AppComponent.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .appId(appId)
            .type(AppComponentType.REPO.name())
            .name(getName())
            .typeDetail(JSONObject.toJSONString(JsonUtil.map(
                "url", url,
                "branch", branch,
                "dockerfileTemplate", dockerfileTemplate,
                "teamRegistryId", teamRegistryId,
                "teamRepoId", teamRepoId
            )))
            .detail(YamlUtil.toJson(detailYaml))
            .description(getDescription())
            .build();
    }

}
