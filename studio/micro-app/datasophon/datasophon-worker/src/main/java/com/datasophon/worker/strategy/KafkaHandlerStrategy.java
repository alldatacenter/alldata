package com.datasophon.worker.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.handler.ServiceHandler;
import com.datasophon.worker.utils.KerberosUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KafkaHandlerStrategy implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(KafkaHandlerStrategy.class);

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler();
        if(command.getEnableKerberos()){
            logger.info("start to get hive keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            KerberosUtils.createKeytabDir();
            if(!FileUtil.exist("/etc/security/keytab/kafka.service.keytab")){
                KerberosUtils.downloadKeytabFromMaster("kafka/" + hostname, "kafka.service.keytab");
            }
        }
        startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(), command.getDecompressPackageName(),command.getRunAs());
        return startResult;
    }
}
