package com.alibaba.tesla.appmanager.meta.helm.repository.domain;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.dto.EnvMetaDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
    * Helm 元信息表
    */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HelmMetaDO {
    /**
    * ID
    */
    private Long id;

    /**
    * 创建时间
    */
    private Date gmtCreate;

    /**
    * 最后修改时间
    */
    private Date gmtModified;

    /**
    * 应用 ID
    */
    private String appId;

    /**
    * Helm 包标识 ID
    */
    private String helmPackageId;

    /**
    * Helm 名称
    */
    private String name;

    /**
    * 描述信息
    */
    private String description;

    /**
    * 组件类型
    */
    private String componentType;

    /**
    * 包类型
    */
    private String packageType;

    /**
    * Helm 扩展信息
    */
    private String helmExt;

    /**
    * 构建 Options 信息
    */
    private String options;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    public JSONObject getJSONOptions(List<EnvMetaDTO> appEnvList) {
        Yaml yaml = SchemaUtil.createYaml(JSONObject.class);
        JSONObject root = yaml.loadAs(options, JSONObject.class);
        JSONObject optionsJson = root.getJSONObject("options");

        JSONArray compEnvList = optionsJson.getJSONArray("env");

        List<String> allEnvList = appEnvList.stream().map(EnvMetaDTO::getName).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(compEnvList)){
            for (int i = 0; i < compEnvList.size(); i++) {
                allEnvList.add(compEnvList.getString(i));
            }
        }

        List<String> allDistinctEnvList = allEnvList.parallelStream().distinct().collect(Collectors.toList());
        optionsJson.put("env", allDistinctEnvList);
        return optionsJson;
    }

    public void setOptionsByHelmExt(JSONObject repo) {
        JSONObject options = new JSONObject();

        options.put("repoPath", repo.getString("repoPath"));
        options.put("branch", repo.getString("branch"));
        options.put("repo", repo.getString("repo"));
        options.put("ciAccount", repo.getString("ciAccount"));
        options.put("ciToken", repo.getString("ciToken"));

        options.put("repoUrl", repo.getString("repoUrl"));
        options.put("chartName", repo.getString("chartName"));
        options.put("chartVersion", repo.getString("chartVersion"));

        JSONObject result = new JSONObject();
        result.put("options", options);

        Yaml yaml = SchemaUtil.createYaml(JSONObject.class);
        this.options = yaml.dumpAsMap(result);
    }
}