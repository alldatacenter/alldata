package com.alibaba.sreworks.appdev.server.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.App;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author jinghua.yjh
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppCreateParam extends AppParam {

    private Long teamId;

    private String name;

    private String description;

    private String annotationsYaml;

    private String labelsYaml;

    private JSONObject annotationsJSONObject;

    private JSONObject labelsJSONObject;

    public App toApp(String operator) throws JsonProcessingException {
        String annotations = annotationsJSONObject != null ?
            JSONObject.toJSONString(annotationsJSONObject) : YamlUtil.toJson(annotationsYaml);
        String labels = labelsJSONObject != null ?
            JSONObject.toJSONString(labelsJSONObject) : YamlUtil.toJson(labelsYaml);

        return App.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .teamId(teamId)
            .name(name)
            .detail(toDetail())
            .description(description)
            .annotations(annotations)
            .labels(labels)
            .display(1L)
            .build();
    }

}
