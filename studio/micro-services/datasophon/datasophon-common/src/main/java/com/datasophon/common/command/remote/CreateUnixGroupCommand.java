package com.datasophon.common.command.remote;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateUnixGroupCommand implements Serializable {
    private String groupName;
}
