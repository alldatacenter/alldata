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

public class DecompressWorkerHandler implements DispatcherWorkerHandler {

    private static final Logger logger = LoggerFactory.getLogger(DecompressWorkerHandler.class);

    @Override
    public boolean handle(ClientSession session, HostInfo hostInfo) {
        String decompressResult = MinaUtils.execCmdWithResult(session, Constants.UNZIP_DDH_WORKER_CMD);
        if (Constants.FAILED.equals(decompressResult)) {
            logger.error("tar -zxvf datasophon-worker.tar.gz failed");
            hostInfo.setErrMsg("tar -zxvf datasophon-worker.tar.gz failed");
            hostInfo.setMessage(MessageResolverUtils.getMessage("decompress.installation.package.fail"));
            CommonUtils.updateInstallState(InstallState.FAILED, hostInfo);
            return false;
        }
        logger.info("decompress datasophon-worker.tar.gz success");
        hostInfo.setProgress(50);
        hostInfo.setMessage(MessageResolverUtils.getMessage("installation.package.decompressed.success.and.modify.configuration.file"));
        return true;
    }
}
