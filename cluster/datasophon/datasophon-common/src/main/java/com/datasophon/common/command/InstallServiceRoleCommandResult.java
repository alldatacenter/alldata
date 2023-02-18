package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class InstallServiceRoleCommandResult extends BaseCommandResult  implements Serializable {


    private static final long serialVersionUID = -2524637560247096696L;
    private InstallServiceRoleCommand installServiceRoleCommand;

    private Long deliveryId;
}
