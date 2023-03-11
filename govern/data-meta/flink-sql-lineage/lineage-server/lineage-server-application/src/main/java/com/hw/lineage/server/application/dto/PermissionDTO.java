package com.hw.lineage.server.application.dto;

import com.hw.lineage.server.application.dto.basic.RootDTO;
import lombok.Data;

/**
 * @description: PermissionDTO
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class PermissionDTO extends RootDTO {

    private Long permissionId;

    private String permissionGroup;

    private String permissionName;

    private String permissionCode;
}
