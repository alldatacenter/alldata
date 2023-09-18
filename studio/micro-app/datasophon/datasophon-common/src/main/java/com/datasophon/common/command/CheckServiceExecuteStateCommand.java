package com.datasophon.common.command;

import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceNode;
import lombok.Data;

import java.io.Serializable;

@Data
public class CheckServiceExecuteStateCommand implements Serializable {

    private ServiceNode serviceNode;

    private ServiceRoleType serviceRoleType;

    public CheckServiceExecuteStateCommand(ServiceNode serviceNode, ServiceRoleType serviceRoleType) {
        this.serviceNode = serviceNode;
        this.serviceRoleType = serviceRoleType;
    }
}
