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

import java.util.ArrayList;

public class NameNodeHandlerStrategy implements ServiceRoleStrategy {
    private static final Logger logger = LoggerFactory.getLogger(NameNodeHandlerStrategy.class);

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        logger.info("namenode start before");
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler();
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            if (command.isSlave()) {
                //执行hdfs namenode -bootstrapStandby
                logger.info("start to execute hdfs namenode -bootstrapStandby");
                ArrayList<String> commands = new ArrayList<>();
                commands.add(workPath + "/bin/hdfs");
                commands.add("namenode");
                commands.add("-bootstrapStandby");
                ExecResult execResult = ShellUtils.execWithStatus(workPath, commands, 30L);
                if (execResult.getExecResult()) {
                    logger.info("namenode standby success");
                } else {
                    logger.info("namenode standby failed");
                    return execResult;
                }
            } else {
                logger.info("start to execute format namenode");
                ArrayList<String> commands = new ArrayList<>();
                commands.add(workPath + "/bin/hdfs");
                commands.add("namenode");
                commands.add("-format");
                commands.add("smhadoop");
                //清空namenode元数据
                FileUtil.del("/data/dfs/nn/current");
                ExecResult execResult = ShellUtils.execWithStatus(workPath, commands, 180L);
                if (execResult.getExecResult()) {
                    logger.info("namenode format success");
                } else {
                    logger.info("namenode format failed");
                    return execResult;
                }
            }
        }
        if (command.getEnableRangerPlugin()) {
            logger.info("start to enable ranger hdfs plugin");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("sh");
            commands.add(workPath + "/ranger-hdfs-plugin/enable-hdfs-plugin.sh");
            if (!FileUtil.exist(workPath + "/ranger-hdfs-plugin/success.id")) {
                ExecResult execResult = ShellUtils.execWithStatus(workPath + "/ranger-hdfs-plugin", commands, 30L);
                if (execResult.getExecResult()) {
                    logger.info("enable ranger hdfs plugin success");
                    //写入ranger plugin集成成功标识
                    FileUtil.writeUtf8String("success", workPath + "/ranger-hdfs-plugin/success.id");
                } else {
                    logger.info("enable ranger hdfs plugin failed");
                    return execResult;
                }
            }
        }
        if (command.getEnableKerberos()) {
            logger.info("start to get namenode keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            KerberosUtils.createKeytabDir();
            if(!FileUtil.exist("/etc/security/keytab/nn.service.keytab")){
                KerberosUtils.downloadKeytabFromMaster("nn/" + hostname, "nn.service.keytab");
            }
            if(!FileUtil.exist("/etc/security/keytab/spnego.service.keytab")){
                KerberosUtils.downloadKeytabFromMaster("HTTP/" + hostname, "spnego.service.keytab");
            }
        }
        startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(), command.getRunAs());

        return startResult;
    }


}
