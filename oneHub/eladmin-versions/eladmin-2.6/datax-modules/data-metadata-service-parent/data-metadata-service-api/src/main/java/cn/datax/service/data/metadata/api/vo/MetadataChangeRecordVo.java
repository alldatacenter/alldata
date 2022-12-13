package cn.datax.service.data.metadata.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 元数据变更记录表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-30
 */
@Data
public class MetadataChangeRecordVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private Integer version;
    private String objectType;
    private String objectId;
    private String fieldName;
    private String fieldOldValue;
    private String fieldNewValue;
    private String sourceId;
    private String sourceName;
    private String tableId;
    private String tableName;
}
