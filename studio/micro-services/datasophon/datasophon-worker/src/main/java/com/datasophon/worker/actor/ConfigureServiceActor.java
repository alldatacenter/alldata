package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.GenerateServiceConfigCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.handler.ConfigureServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigureServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(ConfigureServiceActor.class);
    @Override
    public void onReceive(Object msg) throws Throwable {
        if(msg instanceof GenerateServiceConfigCommand){

            GenerateServiceConfigCommand command = (GenerateServiceConfigCommand) msg;
            logger.info("start configure {}",command.getServiceName());
            ConfigureServiceHandler serviceHandler = new ConfigureServiceHandler();
            ExecResult startResult = serviceHandler.configure(command.getCofigFileMap(),
                                                              command.getDecompressPackageName(),
                                                              command.getMyid(),
                                                              command.getServiceRoleName(),
                                                              command.getRunAs());
            getSender().tell(startResult,getSelf());

            logger.info("{} configure result {}",command.getServiceName(),startResult.getExecResult()?"success":"failed");
        }else {
            unhandled(msg);
        }
    }
}
