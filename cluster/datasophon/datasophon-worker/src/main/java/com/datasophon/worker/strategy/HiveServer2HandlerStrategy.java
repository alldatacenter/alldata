package com.datasophon.worker.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;
import com.datasophon.worker.utils.KerberosUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class HiveServer2HandlerStrategy implements ServiceRoleStrategy {
    private static final Logger logger = LoggerFactory.getLogger(HiveServer2HandlerStrategy.class);

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler();
        if (command.getEnableRangerPlugin()) {
            logger.info("start to enable hive hdfs plugin");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("sh");
            commands.add("./enable-hive-plugin.sh");
            if (!FileUtil.exist(Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + "/ranger-hive-plugin/success.id")) {
                ExecResult execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + "/ranger-hive-plugin", commands, 30L);
                if (execResult.getExecResult()) {
                    logger.info("enable ranger hive plugin success");
                    FileUtil.writeUtf8String("success", Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + "/ranger-hive-plugin/success.id");
                } else {
                    logger.info("enable ranger hive plugin failed");
                    return execResult;
                }
            }
        }
        logger.info("command is slave : {}",command.isSlave());
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE) && !command.isSlave()) {
            //init hive database
            logger.info("start to init hive schema");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("bin/schematool");
            commands.add("-dbType");
            commands.add("mysql");
            commands.add("-initSchema");
            ExecResult execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName(), commands, 60L);
            if (execResult.getExecResult()) {
                logger.info("init hive schema success");
            } else {
                logger.info("init hive schema failed");
                return execResult;
            }
        }
        if(command.getEnableKerberos()){
            logger.info("start to get hive keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            KerberosUtils.createKeytabDir();
            if(!FileUtil.exist("/etc/security/keytab/hive.service.keytab")){
                KerberosUtils.downloadKeytabFromMaster("hive/" + hostname, "hive.service.keytab");
            }
        }
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            String hadoopHome = PropertyUtils.getString("HADOOP_HOME");
            ShellUtils.exceShell("sudo -u hdfs "+hadoopHome+"/bin/hdfs dfs -mkdir -p /user/hive/warehouse");
            ShellUtils.exceShell("sudo -u hdfs "+hadoopHome+"/bin/hdfs dfs -mkdir -p /tmp/hive");
            ShellUtils.exceShell("sudo -u hdfs "+hadoopHome+"/bin/hdfs dfs -chown hive:hadoop /user/hive/warehouse");
            ShellUtils.exceShell("sudo -u hdfs "+hadoopHome+"/bin/hdfs dfs -chown hive:hadoop /tmp/hive");
            ShellUtils.exceShell("sudo -u hdfs "+hadoopHome+"/bin/hdfs dfs -chmod 777 /tmp/hive");
        }

        startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(),command.getRunAs());
        return startResult;
    }
}
