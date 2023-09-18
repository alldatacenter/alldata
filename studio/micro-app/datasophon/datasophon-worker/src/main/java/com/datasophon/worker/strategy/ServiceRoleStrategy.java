package com.datasophon.worker.strategy;

import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;

import java.sql.SQLException;

public interface ServiceRoleStrategy {
    public ExecResult handler(ServiceRoleOperateCommand command ) throws SQLException, ClassNotFoundException;
}
