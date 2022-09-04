package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 组件包任务查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageTaskQueryCondition extends BaseCondition {

    /**
     * 组件包任务主键 ID
     */
    private Long id;

    /**
     * 组件包任务主键 ID 列表
     */
    private List<Long> idList;

    /**
     * 应用打包任务 ID
     */
    private Long appPackageTaskId;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Namespace ID 不等条件
     */
    private String namespaceIdNotEqualTo;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * Stage ID 不等条件
     */
    private String stageIdNotEqualTo;

    /**
     * 组件类型
     */
    private String componentType;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 包版本
     */
    private String packageVersion;

    /**
     * 包创建人
     */
    private String packageCreator;

    /**
     * 任务状态
     */
    private String taskStatus;
}
