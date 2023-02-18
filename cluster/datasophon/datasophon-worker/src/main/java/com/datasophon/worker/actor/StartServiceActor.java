package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.strategy.ServiceRoleStrategy;
import com.datasophon.worker.strategy.ServiceRoleStrategyContext;
import com.datasophon.worker.handler.ServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class StartServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(StartServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof ServiceRoleOperateCommand) {
            ServiceRoleOperateCommand command = (ServiceRoleOperateCommand) msg;
            logger.info("start to start service role {}", command.getServiceRoleName());
            ExecResult startResult = new ExecResult();
            ServiceHandler serviceHandler = new ServiceHandler();

            ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(command.getServiceRoleName());
            if(Objects.nonNull(serviceRoleHandler) ){
                startResult = serviceRoleHandler.handler(command);
            }else{
                startResult = serviceHandler.start(command.getStartRunner(),command.getStatusRunner(),command.getDecompressPackageName(), command.getRunAs());
            }

            getSender().tell(startResult, getSelf());
            logger.info("service role {} start result {}", command.getServiceRoleName(), startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }

    }
}
