package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;

public class ExecuteCmdActor extends UntypedActor {
    @Override
    public void onReceive(Object msg) throws Throwable {
        if(msg instanceof ExecuteCmdCommand){
            ExecuteCmdCommand command = (ExecuteCmdCommand)msg;
            ExecResult execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH,command.getCommands(),60L);
            getSender().tell(execResult,getSelf());
        }else {
            unhandled(msg);
        }
    }
}
