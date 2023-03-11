package com.hw.lineage.server.application.dto.graph.vertex;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @description: Vertex
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public class Vertex {

    private Integer id;

    private String name;

    private List<Column> columns;

    private Boolean hasUpstream;

    private Boolean hasDownstream;

    private String tableIcon;

    private Integer childrenCnt;
}
