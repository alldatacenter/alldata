package com.datasophon.common.model;

import com.datasophon.common.enums.ServiceRoleType;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateCommandHostMessage implements Serializable {
    private String commandHostId;

    private String hostname;

    private Boolean execResult;

    private ServiceRoleType serviceRoleType;

    private String commandId;
}
