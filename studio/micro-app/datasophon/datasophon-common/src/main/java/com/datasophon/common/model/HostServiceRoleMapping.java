package com.datasophon.common.model;

import lombok.Data;

import java.util.List;
@Data
public class HostServiceRoleMapping {

    private String host;

    private List<String> serviceRoles;
}
