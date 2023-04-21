package org.dromara.cloudeon.dto;
import lombok.Data;

import java.util.List;

@Data
public class AlertMessage {

    private String receiver;
    // resolved or firing
    private String status;
    private List<Alert> alerts;
    private String externalURL;
    private String version;
    private String groupKey;
    private int truncatedAlerts;


}