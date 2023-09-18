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

public class HistoryServerHandlerStrategy implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(HistoryServerHandlerStrategy.class);

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        ServiceHandler serviceHandler = new ServiceHandler();
        if(command.getEnableKerberos()){
            logger.info("start to get jobhistoryserver keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            KerberosUtils.createKeytabDir();
            if(!FileUtil.exist("/etc/security/keytab/jhs.service.keytab")){
                KerberosUtils.downloadKeytabFromMaster("jhs/" + hostname, "jhs.service.keytab");
            }
        }
        String hadoopHome = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            //create tmp
            ShellUtils.exceShell("sudo -u hdfs "+hadoopHome+"/bin/hdfs dfs -mkdir -p /user/yarn/yarn-logs");
            ShellUtils.exceShell("sudo -u hdfs "+hadoopHome+"/bin/hdfs dfs -mkdir /tmp");
            ShellUtils.exceShell("sudo -u hdfs "+hadoopHome+"/bin/hdfs dfs -chmod 777 /tmp");
            ShellUtils.exceShell("sudo -u hdfs "+hadoopHome+"/bin/hdfs dfs -chown yarn:hadoop /user/yarn/yarn-logs");
        }
        return serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(), command.getRunAs());
    }
}
