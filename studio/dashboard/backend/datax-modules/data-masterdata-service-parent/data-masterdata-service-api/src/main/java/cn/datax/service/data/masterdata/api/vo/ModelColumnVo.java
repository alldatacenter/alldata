package cn.datax.service.data.masterdata.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 主数据模型列信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Data
public class ModelColumnVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String modelId;
    private String columnName;
    private String columnComment;
    private String columnType;
    private String columnLength;
    private String columnScale;
    private String defaultValue;
    private String isSystem;
    private String isPk;
    private String isRequired;
    private String isInsert;
    private String isEdit;
    private String isDetail;
    private String isList;
    private String isQuery;
    private String queryType;
    private String isBindDict;
    private String bindDictTypeId;
    private String bindDictColumn;
    private String htmlType;
    private Integer sort;
}
