package com.hw.lineage.server.application.dto.graph.link.basic;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: Link
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@NoArgsConstructor
public abstract class Link {

    private Integer id;

    private Integer relU;

    private Integer relV;

    public Link(Integer id, Integer relU, Integer relV) {
        this.id = id;
        this.relU = relU;
        this.relV = relV;
    }
}
