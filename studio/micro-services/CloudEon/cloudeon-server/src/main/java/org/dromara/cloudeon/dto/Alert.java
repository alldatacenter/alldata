package org.dromara.cloudeon.dto;
import lombok.Data;
@Data
public class Alert {

    private String status;
    private AlertLabels labels;
    private String startsAt;
    private String endsAt;
    private String generatorURL;
    private String fingerprint;
    private Annotations annotations;
}