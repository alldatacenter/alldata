package com.datasophon.worker.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;
import com.datasophon.worker.utils.KerberosUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;

public class JournalNodeHandlerStrategy implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(JournalNodeHandlerStrategy.class);


    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler();
        if (command.getEnableKerberos()) {
            logger.info("start to get journalnode keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            KerberosUtils.createKeytabDir();
            String hadoopConfDir = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + "/etc/hadoop/";
            if (!FileUtil.exist(hadoopConfDir + "ssl-server.xml")) {
                ShellUtils.exceShell("cp "+hadoopConfDir+"ssl-server.xml.example ssl-server.xml");
            }
            if (!FileUtil.exist(hadoopConfDir + "ssl-client.xml")) {
                ShellUtils.exceShell("cp "+hadoopConfDir+"ssl-client.xml.example ssl-client.xml");
            }
            if (!FileUtil.exist("/etc/security/keytab/jn.service.keytab")) {
                KerberosUtils.downloadKeytabFromMaster("jn/" + hostname, "jn.service.keytab");
            }
            if (!FileUtil.exist("/etc/security/keytab/keystore")) {
                ArrayList<String> commands = new ArrayList<>();
                commands.add("sh");
                commands.add("keystore.sh");
                commands.add(hostname);
                ExecResult execResult = ShellUtils.execWithStatus(Constants.WORKER_SCRIPT_PATH, commands, 30L);
                if (!execResult.getExecResult()) {
                    logger.info("generate keystore file failed");
                    return execResult;
                }
            }
            startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(), command.getRunAs());
        } else {
            startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(), command.getRunAs());
        }
        return startResult;
    }
}
