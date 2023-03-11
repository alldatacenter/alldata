package com.hw.lineage.server.domain.query.user;

import com.hw.lineage.server.domain.query.PageOrderCriteria;
import lombok.Data;

/**
 * @description: UserQuery
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UserQuery extends PageOrderCriteria {

    private String username;
}