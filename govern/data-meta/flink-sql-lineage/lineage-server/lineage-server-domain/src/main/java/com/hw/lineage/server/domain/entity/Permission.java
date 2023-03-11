package com.hw.lineage.server.domain.entity;

import com.hw.lineage.server.domain.entity.basic.RootEntity;
import com.hw.lineage.server.domain.repository.basic.Entity;
import com.hw.lineage.server.domain.vo.PermissionId;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: Permission
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public class Permission extends RootEntity implements Entity {

    private PermissionId permissionId;

    private String permissionGroup;

    private String permissionName;

    private String permissionCode;
}
