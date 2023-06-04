package com.datasophon.api.master.handler.host;

import cn.hutool.core.io.FileUtil;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public class CheckWorkerMd5Handler implements DispatcherWorkerHandler{
    private static final Logger logger = LoggerFactory.getLogger(CheckWorkerMd5Handler.class);
    @Override
    public boolean handle(ClientSession session, HostInfo hostInfo) {
        String checkWorkerMd5Result = MinaUtils.execCmdWithResult(session,Constants.CHECK_WORKER_MD5_CMD).trim();
        String md5 = FileUtil.readString(
                Constants.MASTER_MANAGE_PACKAGE_PATH +
                        Constants.SLASH +
                        Constants.WORKER_PACKAGE_NAME + ".md5",
                Charset.defaultCharset()).trim();
        logger.info("{} worker package md5 value is : {}",hostInfo.getHostname(),md5);
        if(!md5.equals(checkWorkerMd5Result)){
            logger.error("worker package md5 check failed");
            hostInfo.setErrMsg("worker package md5 check failed");
            hostInfo.setMessage(MessageResolverUtils.getMessage("md5.check.failed"));
            CommonUtils.updateInstallState(InstallState.FAILED, hostInfo);
            return false;
        }
        hostInfo.setProgress(35);
        hostInfo.setMessage(MessageResolverUtils.getMessage("md5.verification.successful.and.installation.package.decompressed"));
        return true;
    }
}
