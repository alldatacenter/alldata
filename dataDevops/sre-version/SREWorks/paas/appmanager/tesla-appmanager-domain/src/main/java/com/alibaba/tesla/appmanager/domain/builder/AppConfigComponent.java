package com.alibaba.tesla.appmanager.domain.builder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigComponent {

    /**
     * Revision 名称 (格式：$componentType|$componentName|$version)
     */
    private String revisionName;

    /**
     * 组件 Scopes
     */
    private List<AppConfigComponentScope> scopes;

    /**
     * 组件 Parameter Values
     */
    private List<AppConfigParameterValue> parameterValues;
}
