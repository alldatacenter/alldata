package com.datasophon.common.model;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceRoleType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class ServiceRoleInfo implements Serializable,Comparable<ServiceRoleInfo> {

    private Integer id;

    private String name;

    private ServiceRoleType roleType;

    private String cardinality;

    private Integer sortNum;

    private ServiceRoleRunner startRunner;

    private ServiceRoleRunner stopRunner;

    private ServiceRoleRunner statusRunner;

    private ExternalLink externalLink;

    private String hostname;

    private String hostCommandId;

    private Integer clusterId;

    private String parentName;

    private String packageName;

    private String decompressPackageName;

    private Map<Generators, List<ServiceConfig>> configFileMap;

    private String logFile;

    private String jmxPort;

    private boolean isSlave = false;

    private CommandType commandType;

    private String masterHost;

    private Boolean enableRangerPlugin;

    private Integer serviceInstanceId;

    private RunAs runAs;


    @Override
    public int compareTo(ServiceRoleInfo serviceRoleInfo) {
        if(Objects.nonNull(serviceRoleInfo.getSortNum()) && Objects.nonNull(this.getSortNum())){
            return this.sortNum - serviceRoleInfo.getSortNum();
        }
        return 0;
    }
}
