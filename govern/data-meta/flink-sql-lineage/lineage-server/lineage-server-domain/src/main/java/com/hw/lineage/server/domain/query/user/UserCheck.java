package com.hw.lineage.server.domain.query.user;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description: UserCheck
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UserCheck  {

    @NotNull
    private String username;
}

