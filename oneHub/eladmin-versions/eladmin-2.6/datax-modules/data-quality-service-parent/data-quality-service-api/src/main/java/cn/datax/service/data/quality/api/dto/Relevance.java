package cn.datax.service.data.quality.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 关联性
 */
@Data
public class Relevance implements Serializable {

    private static final long serialVersionUID = 1L;

    private String relatedTableId;
    private String relatedTable;
    private String relatedTableComment;
    private String relatedColumnId;
    private String relatedColumn;
    private String relatedColumnComment;
}
