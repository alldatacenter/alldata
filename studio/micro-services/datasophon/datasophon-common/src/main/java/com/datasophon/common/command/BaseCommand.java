package com.datasophon.common.command;

import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceRoleRunner;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseCommand implements Serializable {


    private static final long serialVersionUID = -1495156573211152639L;
    private String serviceName;

    private String serviceRoleName;

    private ServiceRoleType serviceRoleType;

    private String hostCommandId;

    private String packageName;

    private Integer clusterId;

    private ServiceRoleRunner startRunner;

    private ServiceRoleRunner stopRunner;

    private ServiceRoleRunner statusRunner;

    private ServiceRoleRunner restartRunner;
}
