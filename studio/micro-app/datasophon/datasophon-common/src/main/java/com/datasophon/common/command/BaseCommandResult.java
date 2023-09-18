package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class BaseCommandResult implements Serializable {
    private Boolean execResult;

    private String execOut;
}
