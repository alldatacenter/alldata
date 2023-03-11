package com.hw.lineage.server.domain.entity;

import com.hw.lineage.server.domain.entity.basic.RootEntity;
import com.hw.lineage.server.domain.repository.basic.Entity;
import com.hw.lineage.server.domain.vo.RoleId;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: Role
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public class Role extends RootEntity implements Entity {

    private RoleId roleId;

    private String roleName;
}
