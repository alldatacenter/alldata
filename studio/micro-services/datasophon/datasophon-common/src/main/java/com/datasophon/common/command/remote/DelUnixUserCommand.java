package com.datasophon.common.command.remote;

import lombok.Data;

import java.io.Serializable;

@Data
public class DelUnixUserCommand implements Serializable {

    private String username;

}
