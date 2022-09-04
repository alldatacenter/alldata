package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import com.alibaba.tesla.appmanager.common.enums.AppPackageTaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用元信息查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageTaskQueryCondition extends BaseCondition {

    private Long id;

    private List<Long> idList = new ArrayList<>();

    private String appId;

    private String operator;

    private List<AppPackageTaskStatusEnum> taskStatusList;

    private String envId;
}
