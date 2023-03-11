package com.hw.lineage.server.domain.query.permission;

import com.hw.lineage.server.domain.query.PageOrderCriteria;
import lombok.Data;

/**
 * @description: PermissionQuery
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class PermissionQuery extends PageOrderCriteria {

    private String permissionName;

    private String permissionCode;
}