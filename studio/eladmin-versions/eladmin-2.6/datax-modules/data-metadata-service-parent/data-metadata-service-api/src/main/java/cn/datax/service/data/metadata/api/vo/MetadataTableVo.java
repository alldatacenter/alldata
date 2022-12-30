package cn.datax.service.data.metadata.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 数据库表信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Data
public class MetadataTableVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String sourceId;
    private String tableName;
    private String tableComment;
    private String sourceName;
}
