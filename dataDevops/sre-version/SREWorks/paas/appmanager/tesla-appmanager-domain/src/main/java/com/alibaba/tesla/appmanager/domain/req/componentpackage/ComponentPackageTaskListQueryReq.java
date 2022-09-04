package com.alibaba.tesla.appmanager.domain.req.componentpackage;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 查询指定 component package 任务运行状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageTaskListQueryReq extends BaseRequest {

    /**
     * 应用打包任务ID
     */
    private Long appPackageTaskId;

    /**
     * component package 任务 ID 列表
     */
    private List<Long> componentPackageTaskIdList;
}
