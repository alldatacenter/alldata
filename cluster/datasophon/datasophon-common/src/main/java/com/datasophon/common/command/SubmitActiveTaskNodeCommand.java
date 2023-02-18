package com.datasophon.common.command;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceNode;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class SubmitActiveTaskNodeCommand implements Serializable {

    private static final long serialVersionUID = 3733897759707096649L;

    private CommandType commandType;
    private Integer clusterId;
    private String clusterCode;
    private DAGGraph<String, ServiceNode, String> dag;
    private Map<String, String> errorTaskList;
    private Map<String, ServiceExecuteState> activeTaskList;
    private Map<String, String>  readyToSubmitTaskList;
    private Map<String, String>  completeTaskList;

}
