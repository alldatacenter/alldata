package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class InstallServiceRoleCommandConfirm implements Serializable {
    private Long deliveryId;
}
