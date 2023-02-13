package com.alibaba.tesla.appmanager.server.lib.helper;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.tesla.dag.api.DagInstApiService;
import com.alibaba.tesla.dag.model.domain.TcDagInstNode;
import com.alibaba.tesla.dag.schedule.status.DagInstNodeStatus;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DAG 助手
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class DagHelper {

    @Autowired
    private DagInstApiService dagInstApiService;

    /**
     * 收集 DAG Inst 中的失败日志
     *
     * @param dagInstId     DAG 实例 ID
     * @param errorMessages 需要填充的错误信息列表
     */
    public void collectExceptionMessages(Long dagInstId, JSONArray errorMessages) throws Exception {
        assert errorMessages != null;
        List<TcDagInstNode> nodeDetails = dagInstApiService.nodes(dagInstId);
        for (TcDagInstNode node : nodeDetails) {
            if (DagInstNodeStatus.EXCEPTION.equals(node.status())) {
                switch (node.type()) {
                    case DAG:
                        collectExceptionMessages(node.getSubDagInstId(), errorMessages);
                        break;
                    case NODE:
                        String nodeId = node.getNodeId();
                        String details = node.getStatusDetail();
                        if (details == null) {
                            details = "";
                        }
                        errorMessages.add(ImmutableMap.of(
                                "nodeId", nodeId,
                                "details", details
                        ));
                        break;
                    default:
                        log.error("invalid dag node type {}", node.type().toString());
                        break;
                }
            }
        }
    }
}
