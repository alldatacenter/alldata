package org.dromara.cloudeon.controller.request;

import lombok.Data;

@Data
public class SaveNodeRequest {
    private String sshUser;
    private String sshPassword;
    private Integer sshPort;
    private Integer clusterId;
    private String ip;
    private String hostname;
}
