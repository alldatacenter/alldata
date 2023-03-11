package com.hw.lineage.server.application.dto.graph.link;

import com.hw.lineage.server.application.dto.graph.link.basic.Link;
import lombok.Data;

/**
 * @description: ColumnLink
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class ColumnLink extends Link {

    private Integer u;

    private Integer v;

    private String transform;

    public ColumnLink(Integer id, Integer relU, Integer relV, Integer u, Integer v, String transform) {
        super(id, relU, relV);
        this.u = u;
        this.v = v;
        this.transform = transform;
    }
}
