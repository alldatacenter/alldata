package com.datasophon.common.model;

import lombok.Data;

@Data
public class ProcInfo {
    private String HostName;

    private Boolean Alive;

    private String ErrMsg;
}
