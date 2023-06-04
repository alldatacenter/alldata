package com.datasophon.api.master;

import akka.actor.UntypedActor;
import cn.hutool.core.util.ObjectUtil;
import com.datasophon.api.master.handler.host.*;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.DispatcherHostAgentCommand;
import com.datasophon.common.model.HostInfo;
import org.apache.commons.lang.ObjectUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.nio.file.Files;
import java.nio.file.Paths;


public class DispatcherWorkerActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherWorkerActor.class);

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        logger.info("host actor restart because {}", reason.getMessage());
        super.preRestart(reason, message);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        DispatcherHostAgentCommand command = (DispatcherHostAgentCommand) message;
        HostInfo hostInfo = command.getHostInfo();
        logger.info("start dispatcher host agent :{}", hostInfo.getHostname());
        hostInfo.setMessage(MessageResolverUtils.getMessage("distributed.host.management.agent.installation.package"));
        ClientSession session = MinaUtils.openConnection(
                hostInfo.getHostname(),
                hostInfo.getSshPort(),
                hostInfo.getSshUser(),
                Constants.SLASH + hostInfo.getSshUser() + Constants.ID_RSA);
        DispatcherWorkerHandlerChain handlerChain = new DispatcherWorkerHandlerChain();
        handlerChain.addHandler(new UploadWorkerHandler());
        handlerChain.addHandler(new CheckWorkerMd5Handler());
        handlerChain.addHandler(new DecompressWorkerHandler());
        handlerChain.addHandler(new InstallJDKHandler());
        handlerChain.addHandler(new StartWorkerHandler(command.getClusterId(), command.getClusterFrame()));
        handlerChain.handle(session, hostInfo);
        if (ObjectUtil.isNotEmpty(session)) {
            session.close();
        }
    }
}
