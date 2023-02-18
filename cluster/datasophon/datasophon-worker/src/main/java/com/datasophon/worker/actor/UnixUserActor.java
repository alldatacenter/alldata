package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.remote.CreateUnixUserCommand;
import com.datasophon.common.command.remote.DelUnixUserCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.utils.UnixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UnixUserActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger(UnixUserActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof CreateUnixUserCommand) {
            CreateUnixUserCommand command = (CreateUnixUserCommand) msg;
            ExecResult execResult = UnixUtils.createUnixUser(command.getUsername(), command.getMainGroup(), command.getOtherGroups());
            logger.info("create unix user {}",execResult.getExecResult() ? "success" : "failed");
            getSender().tell(execResult,getSelf());
        }else if (msg instanceof DelUnixUserCommand) {
            DelUnixUserCommand command = (DelUnixUserCommand) msg;
            ExecResult execResult = UnixUtils.delUnixUser(command.getUsername());
            logger.info("del unix user {}",execResult.getExecResult() ? "success" : "failed");
            getSender().tell(execResult,getSelf());
        }
        else {
            unhandled(msg);
        }
    }
}
