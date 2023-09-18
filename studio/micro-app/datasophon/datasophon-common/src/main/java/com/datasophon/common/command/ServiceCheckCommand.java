package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class ServiceCheckCommand extends BaseCommand implements Serializable {
    private Integer serviceRoleInstanceId;

    private Long deliveryId;
}
