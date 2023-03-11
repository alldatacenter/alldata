package com.hw.lineage.server.domain.query.role;

import com.hw.lineage.server.domain.query.PageOrderCriteria;
import lombok.Data;

/**
 * @description: RoleQuery
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class RoleQuery extends PageOrderCriteria {

    private String roleName;
}