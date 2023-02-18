package com.datasophon.api.master.handler.host;

import com.datasophon.api.utils.CommonUtils;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadWorkerHandler implements DispatcherWorkerHandler {
    private static final Logger logger = LoggerFactory.getLogger(StartWorkerHandler.class);


    @Override
    public boolean handle(ClientSession session, HostInfo hostInfo) {
        boolean uploadFile = MinaUtils.uploadFile(session,
                Constants.INSTALL_PATH,
                Constants.MASTER_MANAGE_PACKAGE_PATH +
                        Constants.SLASH +
                        Constants.WORKER_PACKAGE_NAME);
        if(uploadFile){
            hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.successful.and.starts.md5.authentication"));
            hostInfo.setProgress(25);
        }else{
            hostInfo.setMessage(MessageResolverUtils.getMessage("distributed.host.management.agent.installation.package.fail"));
            hostInfo.setErrMsg("dispatcher host agent to " + hostInfo.getHostname() + " failed");
            CommonUtils.updateInstallState(InstallState.FAILED, hostInfo);
        }
        return uploadFile;
    }
}
