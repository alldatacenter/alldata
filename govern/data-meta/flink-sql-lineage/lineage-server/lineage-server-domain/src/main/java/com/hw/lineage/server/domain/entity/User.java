package com.hw.lineage.server.domain.entity;

import com.hw.lineage.server.domain.entity.basic.RootEntity;
import com.hw.lineage.server.domain.repository.basic.Entity;
import com.hw.lineage.server.domain.vo.UserId;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: User
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public class User extends RootEntity implements Entity {

    private UserId userId;

    private String username;

    private String password;

    private Boolean locked;

}
