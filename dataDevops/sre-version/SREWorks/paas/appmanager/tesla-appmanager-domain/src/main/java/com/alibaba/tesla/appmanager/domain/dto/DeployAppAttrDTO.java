package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 部署 App 工单属性 DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppAttrDTO {

    private Long deployAppId;

    private String deployErrorMessage;

    private String deployStatus;

    private Map<String, String> attrMap;
}
