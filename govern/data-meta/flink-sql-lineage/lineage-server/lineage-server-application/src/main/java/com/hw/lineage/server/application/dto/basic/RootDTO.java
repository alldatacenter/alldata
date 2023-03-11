package com.hw.lineage.server.application.dto.basic;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: RootDTO
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public abstract class RootDTO implements Serializable  {

    private Long createTime;

    private Long modifyTime;
}
