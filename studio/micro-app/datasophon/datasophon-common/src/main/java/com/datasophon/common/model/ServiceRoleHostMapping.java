package com.datasophon.common.model;

import lombok.Data;

import java.util.List;

@Data
public class ServiceRoleHostMapping {

    private String serviceRole;

    private List<String> hosts;
}
