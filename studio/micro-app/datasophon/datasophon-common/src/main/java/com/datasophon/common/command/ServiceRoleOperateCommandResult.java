package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class ServiceRoleOperateCommandResult extends BaseCommandResult implements Serializable {

    private static final long serialVersionUID = -566450658744102059L;
    private ServiceRoleOperateCommand serviceRoleOperateCommand;
}
