package com.datasophon.common.command;

import com.datasophon.common.model.HostInfo;
import lombok.Data;

@Data
public class DispatcherHostAgentCommand {
    private HostInfo hostInfo;

    private Integer clusterId;

    private String clusterFrame;

    public DispatcherHostAgentCommand(HostInfo hostInfo, Integer clusterId, String clusterFrame) {
        this.hostInfo = hostInfo;
        this.clusterId = clusterId;
        this.clusterFrame = clusterFrame;
    }
}
