package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.handler.ServiceHandler;

public class RestartServiceActor extends UntypedActor {

    @Override
    public void onReceive(Object msg) throws Throwable {
        if(msg instanceof ServiceRoleOperateCommand){
            ServiceRoleOperateCommand command = (ServiceRoleOperateCommand) msg;
            ServiceHandler serviceHandler = new ServiceHandler();
            ExecResult startResult = serviceHandler.reStart(command.getRestartRunner(), command.getDecompressPackageName());
            getSender().tell(startResult,getSelf());
        }else{
            unhandled(msg);
        }
    }
}
