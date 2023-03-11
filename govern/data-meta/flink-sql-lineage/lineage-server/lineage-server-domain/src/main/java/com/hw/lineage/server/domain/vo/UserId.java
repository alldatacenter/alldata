package com.hw.lineage.server.domain.vo;

import com.hw.lineage.server.domain.repository.basic.Identifier;
import lombok.Data;

/**
 * @description: UserId
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UserId implements Identifier {

    private final Long value;

    public UserId(Long value) {
        this.value = value;
    }
}

