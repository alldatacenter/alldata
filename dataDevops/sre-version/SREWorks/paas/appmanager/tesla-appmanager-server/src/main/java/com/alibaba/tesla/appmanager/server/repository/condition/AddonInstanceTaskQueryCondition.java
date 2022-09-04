package com.alibaba.tesla.appmanager.server.repository.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonInstanceTaskQueryCondition implements Serializable {

    private String namespaceId;

    private String addonId;

    private String addonVersion;

    private String addonName;

    private Map<String, String> addonAttrs;

    private List<String> taskStatusList;

    private Long taskProcessId;
}
