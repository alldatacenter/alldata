package com.datasophon.common.model;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ServiceExecuteResultMessage implements Serializable {

    private ServiceExecuteState serviceExecuteState;

    private String serviceName;

    private ServiceRoleType serviceRoleType;

    private Integer clusterId;
    private String clusterCode;
    private CommandType commandType;

    private DAGGraph<String, ServiceNode, String> dag;
    private Map<String, String> errorTaskList;
    private Map<String, ServiceExecuteState> activeTaskList;
    private Map<String, String>  readyToSubmitTaskList;
    private Map<String, String>  completeTaskList;



}
