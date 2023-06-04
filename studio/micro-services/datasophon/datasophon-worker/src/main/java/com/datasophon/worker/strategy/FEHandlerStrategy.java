package com.datasophon.worker.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.ServiceRoleRunner;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.common.utils.StarRocksUtils;
import com.datasophon.common.utils.ThrowableUtils;
import com.datasophon.worker.handler.ServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;

public class FEHandlerStrategy implements ServiceRoleStrategy {
    private static final Logger logger = LoggerFactory.getLogger(FEHandlerStrategy.class);

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command)  {
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler();
        if ( command.getCommandType()== CommandType.INSTALL_SERVICE) {
            if(command.isSlave()){
                logger.info("first start  fe");
                ArrayList<String> commands = new ArrayList<>();
                commands.add("--helper");
                commands.add(command.getMasterHost() + ":9010");
                commands.add("--daemon");

                ServiceRoleRunner startRunner = new ServiceRoleRunner();
                startRunner.setProgram(command.getStartRunner().getProgram());
                startRunner.setArgs(commands);
                startRunner.setTimeout("60");
                startResult = serviceHandler.start(startRunner, command.getStatusRunner(), command.getDecompressPackageName(),command.getRunAs());
                if (startResult.getExecResult()) {
                    //add follower
                    try {
                        StarRocksUtils.allFollower(command.getMasterHost(), CacheUtils.getString(Constants.HOSTNAME));
                    }catch (SQLException | ClassNotFoundException e){
                        logger.info("add slave fe failed {}", ThrowableUtils.getStackTrace(e));
                    }
                    logger.info("slave fe start success");
                } else {
                    logger.info("slave fe start failed");
                }
            }else{
                startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(),command.getRunAs());
            }
        } else {
            startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(),command.getRunAs());
        }
        return startResult;
    }
}
