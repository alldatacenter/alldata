package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Addon信息查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppAddonQueryCondition extends BaseCondition {

    private Long id;

    private String appId;

    private List<ComponentTypeEnum> addonTypeList;

    private String addonId;

    private String addonName;

    private String namespaceId;

    private String namespaceIdNotEqualTo;

    private String stageId;

    private String stageIdNotEqualTo;
}
