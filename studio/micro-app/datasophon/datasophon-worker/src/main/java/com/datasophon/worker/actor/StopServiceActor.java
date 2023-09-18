package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.handler.ServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopServiceActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger(StopServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if(msg instanceof ServiceRoleOperateCommand){
            ServiceRoleOperateCommand command = (ServiceRoleOperateCommand) msg;

            logger.info("start to stop service role {}", command.getServiceRoleName());
            ServiceHandler serviceHandler = new ServiceHandler();
            ExecResult stopResult = serviceHandler.stop(command.getStopRunner(),command.getStatusRunner(),command.getDecompressPackageName(),command.getRunAs());
            getSender().tell(stopResult,getSelf());

            logger.info("service role {} stop result {}", command.getServiceRoleName(), stopResult.getExecResult() ? "success" : "failed");
        }else{
            unhandled(msg);
        }
    }
}
