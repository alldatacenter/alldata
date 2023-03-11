package com.hw.lineage.server.domain.query.role;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description: RoleCheck
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class RoleCheck {

    @NotNull
    private String roleName;
}

