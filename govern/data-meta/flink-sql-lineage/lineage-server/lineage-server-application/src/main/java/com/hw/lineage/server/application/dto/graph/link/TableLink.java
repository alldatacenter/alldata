package com.hw.lineage.server.application.dto.graph.link;

import com.hw.lineage.server.application.dto.graph.link.basic.Link;
import lombok.Data;

/**
 * @description: TableLink
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class TableLink extends Link {

    private String sqlSource;

    public TableLink(Integer id, Integer relU, Integer relV, String sqlSource) {
        super(id, relU, relV);
        this.sqlSource = sqlSource;
    }
}
