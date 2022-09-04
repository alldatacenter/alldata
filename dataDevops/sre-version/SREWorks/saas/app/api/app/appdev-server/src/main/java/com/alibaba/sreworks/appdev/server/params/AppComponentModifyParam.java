package com.alibaba.sreworks.appdev.server.params;

import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.AppComponent;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppComponentModifyParam {

    private String name;

    private String description;

    private String detailYaml;

    public void patchAppComponent(AppComponent appComponent, String operator) throws JsonProcessingException {
        appComponent.setGmtModified(System.currentTimeMillis() / 1000);
        appComponent.setLastModifier(operator);
        appComponent.setName(name);
        appComponent.setDescription(description);
        appComponent.setDetail(YamlUtil.toJson(detailYaml));
    }

}
