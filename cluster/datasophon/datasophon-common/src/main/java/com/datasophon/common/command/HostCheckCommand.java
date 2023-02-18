package com.datasophon.common.command;


import com.datasophon.common.model.HostInfo;
import lombok.Data;

@Data
public class HostCheckCommand {


    private HostInfo hostInfo;

    private String clusterCode;

    public HostCheckCommand(HostInfo hostInfo, String clusterCode) {
        this.hostInfo = hostInfo;
        this.clusterCode = clusterCode;
    }

    public HostCheckCommand() {
    }
}
