package com.datasophon.common.command;

import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class InstallServiceRoleCommand  extends BaseCommand implements Serializable{

    private static final long serialVersionUID = -8610024764701745463L;

    private Map<Generators, List<ServiceConfig>> cofigFileMap;

    private Long deliveryId;

    private Integer normalSize;

    private String packageMd5;

    private String decompressPackageName;

    private RunAs runAs;

    private ServiceRoleType serviceRoleType;

}
