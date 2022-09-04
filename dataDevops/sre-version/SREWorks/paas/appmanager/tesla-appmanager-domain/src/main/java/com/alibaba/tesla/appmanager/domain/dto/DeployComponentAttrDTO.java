package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.tesla.appmanager.common.enums.DeployComponentAttrTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 部署 Component 工单属性 DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployComponentAttrDTO {

    private Long deployComponentId;

    private String deployStatus;

    private String deployErrorMessage;

    private Map<String, String> attrMap;
}
