package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.remote.CreateUnixGroupCommand;
import com.datasophon.common.command.remote.CreateUnixUserCommand;
import com.datasophon.common.command.remote.DelUnixGroupCommand;
import com.datasophon.common.command.remote.DelUnixUserCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.utils.UnixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnixGroupActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(UnixGroupActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof CreateUnixGroupCommand) {
            CreateUnixGroupCommand command = (CreateUnixGroupCommand) msg;
            ExecResult execResult = UnixUtils.createUnixGroup(command.getGroupName());
            logger.info("create unix group {} {}",command.getGroupName(),execResult.getExecResult() ? "success" : "failed");
            getSender().tell(execResult,getSelf());
        }else if (msg instanceof DelUnixGroupCommand) {
            DelUnixGroupCommand command = (DelUnixGroupCommand) msg;
            ExecResult execResult = UnixUtils.delUnixGroup(command.getGroupName());
            logger.info("del unix group {} {}",command.getGroupName(),execResult.getExecResult() ? "success" : "failed");
            getSender().tell(execResult,getSelf());
        }
        else {
            unhandled(msg);
        }
    }
}
