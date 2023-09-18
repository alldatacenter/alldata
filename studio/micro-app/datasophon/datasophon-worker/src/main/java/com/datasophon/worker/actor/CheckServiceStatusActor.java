package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.handler.ServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckServiceStatusActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(CheckServiceStatusActor.class);
    @Override
    public void onReceive(Object msg) throws Throwable {
        if(msg instanceof InstallServiceRoleCommand){
            InstallServiceRoleCommand command = (InstallServiceRoleCommand) msg;
            ServiceHandler serviceHandler = new ServiceHandler();
            ExecResult statusResult = serviceHandler.status(command.getStatusRunner(), command.getDecompressPackageName());
            if(!statusResult.getExecResult()){
                logger.info("{} status failed",command.getDecompressPackageName());
                statusResult.setExecResult(false);
            }
        }else {
            unhandled(msg);
        }
    }
}
