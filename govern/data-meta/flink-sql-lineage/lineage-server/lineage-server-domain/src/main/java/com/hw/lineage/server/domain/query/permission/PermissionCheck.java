package com.hw.lineage.server.domain.query.permission;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description: PermissionCheck
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class PermissionCheck {

    private String permissionName;

    private String permissionCode;
}

