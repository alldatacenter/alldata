package com.hw.lineage.server.application.command.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @description: CreateUserCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class CreateUserCmd {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
