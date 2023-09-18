package com.datasophon.common.command;

import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.model.ServiceRoleInfo;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CheckCommandExecuteProgressCommand {



    private List<ServiceRoleInfo> serviceRoleInfoList;

    private Map<String, ServiceExecuteState> completeTaskList;

    public CheckCommandExecuteProgressCommand(Map<String, ServiceExecuteState> completeTaskList, List<ServiceRoleInfo> serviceRoleInfoList) {
        this.completeTaskList = completeTaskList;
        this.serviceRoleInfoList = serviceRoleInfoList;
    }
}
