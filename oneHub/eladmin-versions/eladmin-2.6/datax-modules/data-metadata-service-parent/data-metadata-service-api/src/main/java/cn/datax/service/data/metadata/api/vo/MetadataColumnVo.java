package cn.datax.service.data.metadata.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 元数据信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Data
public class MetadataColumnVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String sourceId;
    private String tableId;
    private String columnName;
    private String columnComment;
    private String columnKey;
    private String columnNullable;
    private String columnPosition;
    private String dataType;
    private String dataLength;
    private String dataPrecision;
    private String dataScale;
    private String dataDefault;
    private String sourceName;
    private String tableName;
    private String tableComment;
}
