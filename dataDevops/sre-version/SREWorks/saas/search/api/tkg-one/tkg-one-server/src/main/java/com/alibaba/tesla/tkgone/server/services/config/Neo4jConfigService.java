package com.alibaba.tesla.tkgone.server.services.config;

import com.alibaba.tesla.tkgone.server.common.Constant;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjinghua
 */
@Service
public class Neo4jConfigService extends BaseConfigService{

    public int getNeo4jReadTimeout() {
        String name = "neo4jReadTimeout";
        return getNameContentWithDefault(name, Constant.NEO4J_READ_TIMEOUT);
    }

    public int getNeo4jWriteTimeout() {
        String name = "neo4jWriteTimeout";
        return getNameContentWithDefault(name, Constant.NEO4J_WRITE_TIMEOUT);
    }

    public int getNeo4jConnectTimeout() {
        String name = "neo4jConnectTimeout";
        return getNameContentWithDefault(name, Constant.NEO4J_CONNECT_TIMEOUT);
    }

    public List<String> getNeo4jTypeNodeExtraFields(String type) {
        String name = "neo4jTypeNodeExtraFields";
        return getTypeNameContentWithDefault(type, name, new ArrayList<>());
    }

}
