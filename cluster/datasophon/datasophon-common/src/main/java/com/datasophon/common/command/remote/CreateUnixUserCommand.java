package com.datasophon.common.command.remote;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateUnixUserCommand implements Serializable {

    private String username;

    private String mainGroup;

    private String otherGroups;
}
