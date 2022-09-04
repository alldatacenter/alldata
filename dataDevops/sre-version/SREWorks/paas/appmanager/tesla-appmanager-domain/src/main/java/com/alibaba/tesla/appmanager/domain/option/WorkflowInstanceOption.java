package com.alibaba.tesla.appmanager.domain.option;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceOption {

    /**
     * Workflow Task 执行顺序 (不提供该参数则默认全量按需执行)
     * <p>
     * 需要指定 Workflow Task 的数组下标列表（如跳过某几个 Workflow 任务执行节点）
     * <p>
     * 示例：[0, 1, 3]
     */
    private List<Integer> executeOrders;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 计算并获取当前实际的 workflow instance 中的 workflow tasks 的执行顺序
     *
     * @param configuration 部署请求配置信息
     * @return 执行顺序数组下标列表
     */
    public List<Integer> calculateExecuteOrders(DeployAppSchema configuration) {
        // 不提供 executeOrders 参数时按需执行全量 workflow tasks
        if (executeOrders == null || executeOrders.size() == 0) {
            List<Integer> orders = new ArrayList<>();
            for (int i = 0; i < configuration.getSpec().getWorkflow().getSteps().size(); i++) {
                orders.add(i);
            }
            return orders;
        }

        // 检测 executeOrders 合法性，不允许反向执行，不允许越界
        int maxIndex = 0;
        for (int i = 0; i < executeOrders.size(); i++) {
            int index = executeOrders.get(i);
            if (index > maxIndex || index >= executeOrders.size() || index < 0) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        "invalid exeucte order parameters in workflow instance options");
            }
            maxIndex = Math.max(maxIndex, executeOrders.get(i));
        }

        // 注意此处认为如同 [0, 0, 1, 1, 2] 的执行顺序是被允许的，即允许重复相邻节点重复，但必须单调上升
        return executeOrders;
    }
}
