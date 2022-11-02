package com.alibaba.tesla.tkgone.server.services.config;

import com.alibaba.tesla.tkgone.server.common.Constant;
import org.springframework.stereotype.Service;

/**
 * @author yangjinghua
 */
@Service
public class BackendStoreConfigService extends BaseConfigService{

    public int getTypePartitionNums(String type) {
        String name = "typePartitionNums";
        return getTypeNameContentWithDefault(type, name, Constant.DEFAULT_PARTITION_NUM);
    }

    public int getTypeValidTime(String type) {
        String name = "typeTTL";
        return getTypeNameContentWithDefault(type, name, Constant.DEFAULT_TTL);
    }

}
