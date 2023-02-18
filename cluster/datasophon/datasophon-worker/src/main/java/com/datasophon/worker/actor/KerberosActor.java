package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.remote.GenerateKeytabFileCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public class KerberosActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(KerberosActor.class);

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof GenerateKeytabFileCommand) {
            ExecResult execResult = new ExecResult();
            GenerateKeytabFileCommand command = (GenerateKeytabFileCommand) message;
            String keytabFilePath = "/etc/security/keytab/" + command.getHostname() + Constants.SLASH + command.getKeytabName();
            logger.info("find keytab file {}", keytabFilePath);
            if (!FileUtil.exist(keytabFilePath)) {
                FileUtil.mkParentDirs(keytabFilePath);
                String addprinc = "kadmin -padmin/admin -wadmin -q \"addprinc -randkey " + command.getPrincipal() + "\"";
                logger.info("add principal : {}", addprinc);
                ShellUtils.exceShell(addprinc);
                String keytabCmd = "kadmin -padmin/admin -wadmin -q \"xst -k " + keytabFilePath + " " + command.getPrincipal() + "\"";

                logger.info("generate keytab file cmd :{}", keytabCmd);
                ShellUtils.exceShell(keytabCmd);
            }

            String keytabStr = FileUtil.readString(keytabFilePath, Charset.defaultCharset());
            logger.info(keytabStr);
            execResult.setExecResult(true);
            execResult.setExecOut(keytabStr);
            getSender().tell(execResult, getSelf());
        } else {
            unhandled(message);
        }
    }
}
