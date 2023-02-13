package com.alibaba.tesla.tkgone.server.services.config;

import com.alibaba.tesla.tkgone.server.common.Constant;
import org.springframework.stereotype.Service;

/**
 * @author yangjinghua
 */
@Service
public class ConsumerConfigService extends BaseConfigService{

    public int getConsumerNodeHeartBeatInvalidTimeInterval() {
        String name = "consumerNodeHeartBeatInvalidInterval";
        return getNameContentWithDefault(name, Constant.CONSUMER_NODE_HEARTBEAT_INVALID_TIME_INTERVAL);
    }

    public int getConsumerNodeHeartBeatInterval() {
        String name = "consumerNodeHeartBeatInterval";
        return getNameContentWithDefault(name, Constant.CONSUMER_NODE_HEARTBEAT_TIME_INTERVAL);
    }

    public int getMysqlTableEffectiveInterval() {
        String name = "consumerMysqlTableEffectiveInterval";
        return getNameContentWithDefault(name, Constant.CONSUMER_MYSQL_TABLE_EFFECTIVE_INTERVAL);
    }

    public int getODPSTableEffectiveInterval() {
        String name = "consumerODPSTableEffectiveInterval";
        return getNameContentWithDefault(name, Constant.CONSUMER_ODPS_TABLE_EFFECTIVE_INTERVAL);
    }

    public int getLarkDocumentEffectiveInterval() {
        String name = "consumerLarkDocumentEffectiveInterval";
        return getNameContentWithDefault(name, Constant.CONSUMER_LARK_DOCUMENT_EFFECTIVE_INTERVAL);
    }

    public int getScriptEffectiveInterval() {
        String name = "consumerScriptEffectiveInterval";
        return getNameContentWithDefault(name, Constant.CONSUMER_SCRIPT_EFFECTIVE_INTERVAL);
    }

    public int getSingleConsumerMysqlTableConcurrentExecNum() {
        String name = "singleConsumerMysqlTableConcurrentExecNum";
        return getNameContentWithDefault(name, Constant.SINGLE_CONSUMER_MYSQL_TABLE_CONCURRENT_EXEC_NUM);
    }

    public int getSingleConsumerODPSTableConcurrentExecNum() {
        String name = "singleConsumerODPSTableConcurrentExecNum";
        return getNameContentWithDefault(name, Constant.SINGLE_CONSUMER_ODPS_TABLE_CONCURRENT_EXEC_NUM);
    }

    public int getSingleConsumerLarkDocumentConcurrentExecNum() {
        String name = "singleConsumerLarkDocumentConcurrentExecNum";
        return getNameContentWithDefault(name, Constant.SINGLE_CONSUMER_LARK_DOCUMENT_CONCURRENT_EXEC_NUM);
    }

    public int getSingleConsumerTtReaderConcurrentExecNum() {
        String name = "singleConsumerTtReaderConcurrentExecNum";
        return getNameContentWithDefault(name, Constant.SINGLE_CONSUMER_TT_READER_CONCURRENT_EXEC_NUM);
    }

    public int getSingleConsumerScriptConcurrentExecNum() {
        String name = "singleConsumerScriptConcurrentExecNum";
        return getNameContentWithDefault(name, Constant.SINGLE_CONSUMER_SCRIPT_CONCURRENT_EXEC_NUM);
    }

    public int getTTReaderEffectiveInterval() {
        String name = "consumerTTReaderEffectiveInterval";
        return getNameContentWithDefault(name, Constant.CONSUMER_TT_READER_EFFECTIVE_INTERVAL);
    }

    public String getConsumerNodes() {
        return getNameContentWithDefault("consumerNodes", "");
    }
}
