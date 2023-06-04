
package com.datasophon.common.model.alert;
import lombok.Data;

import java.util.List;

@Data
public class AlertMessage {

    private String receiver;
    private String status;
    private List<Alerts> alerts;
    private String externalURL;
    private String version;
    private String groupKey;
    private int truncatedAlerts;


}