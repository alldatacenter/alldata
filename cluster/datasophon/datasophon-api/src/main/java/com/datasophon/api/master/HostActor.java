package com.datasophon.api.master;

import akka.actor.UntypedActor;
import cn.hutool.core.util.ObjectUtil;
import com.datasophon.api.enums.Status;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.HostCheckCommand;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

public class HostActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger(HostActor.class);

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        logger.info("or restart because {}", reason.getMessage());
        super.preRestart(reason, message);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof HostCheckCommand) {
            HostCheckCommand hostCheckCommand = (HostCheckCommand) message;
            HostInfo hostInfo = hostCheckCommand.getHostInfo();
            logger.info("start host check:{}", hostInfo.getHostname());
            ClientSession session =  MinaUtils.openConnection(
                    hostInfo.getHostname(),
                    hostInfo.getSshPort(),
                    hostInfo.getSshUser(),
                    Constants.SLASH + hostInfo.getSshUser() + Constants.ID_RSA);
            if (ObjectUtil.isNotNull(session)) {
                hostInfo.setCheckResult(new CheckResult(Status.CHECK_HOST_SUCCESS.getCode(), Status.CHECK_HOST_SUCCESS.getMsg()));
            } else {
                hostInfo.setCheckResult(new CheckResult(Status.CONNECTION_FAILED.getCode(), Status.CONNECTION_FAILED.getMsg()));
                MinaUtils.closeConnection(session);
            }
            logger.info("end host check:{}", hostInfo.getHostname());
        } else {
            unhandled(message);
        }
    }
}
