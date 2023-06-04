package com.datasophon.common.command;

import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class GenerateServiceConfigCommand implements Serializable {

    private static final long serialVersionUID = -4211566568993105684L;

    private String serviceName;

    private String decompressPackageName;

    private Integer myid;

    Map<Generators, List<ServiceConfig>> cofigFileMap;

    private String serviceRoleName;

    private RunAs runAs;
}
