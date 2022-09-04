package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 附加组件元信息查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AddonMetaQueryCondition extends BaseCondition {

    private String addonId;

    private String addonVersion;

    private List<ComponentTypeEnum> addonTypeList;
}
