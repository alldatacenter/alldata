package com.datasophon.common.command;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExecuteServiceRoleCommand {
    private Integer clusterId;

    private String clusterCode;

    private String serviceName;

    private List<ServiceRoleInfo> masterRoles;

    private ServiceRoleInfo workerRole;

    private ServiceRoleType serviceRoleType;

    private CommandType commandType;

    private DAGGraph<String, ServiceNode, String> dag;

    private Map<String, String> errorTaskList;
    private Map<String, ServiceExecuteState> activeTaskList;
    private Map<String, String>  readyToSubmitTaskList;
    private Map<String, String>  completeTaskList;

    public ExecuteServiceRoleCommand(Integer clusterId, String serviceName, List<ServiceRoleInfo> serviceRoles) {
        this.clusterId = clusterId;
        this.serviceName = serviceName;
        this.masterRoles = serviceRoles;
    }

    public ExecuteServiceRoleCommand() {
    }
}
