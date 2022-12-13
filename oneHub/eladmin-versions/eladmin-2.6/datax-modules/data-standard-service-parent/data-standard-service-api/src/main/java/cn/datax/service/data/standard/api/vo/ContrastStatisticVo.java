package cn.datax.service.data.standard.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 对照表信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
public class ContrastStatisticVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String sourceId;
    private String sourceName;
    private String tableId;
    private String tableName;
    private String tableComment;
    private String columnId;
    private String columnName;
    private String columnComment;
    private String gbTypeId;
    private String gbTypeCode;
    private String gbTypeName;
    private String bindGbColumn;

    private Integer mappingCount;
    private Integer unMappingCount;
    /**
     * 对照比例 20%
     */
    private String mappingPercent;
}
