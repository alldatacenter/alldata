package com.datasophon.worker.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;

public class Krb5KdcHandlerStrategy implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(Krb5KdcHandlerStrategy.class);

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE) &&
                !FileUtil.exist("/var/kerberos/krb5kdc/principal")) {
                ArrayList<String> commands = new ArrayList<>();
                commands.add("sh");
                commands.add(Constants.WORKER_SCRIPT_PATH+"create_kdb.sh");
                ExecResult execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 180L);
                if (execResult.getExecResult()) {
                    logger.info("init kdc database success");
                    startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(), command.getRunAs());
                } else {
                    logger.info("init kdc database failed");
                }
        } else {
            startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(), command.getRunAs());
        }
        return startResult;
    }
}
