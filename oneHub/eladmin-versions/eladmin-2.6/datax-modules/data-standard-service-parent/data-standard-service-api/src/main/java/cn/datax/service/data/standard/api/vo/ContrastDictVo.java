package cn.datax.service.data.standard.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 字典对照信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
public class ContrastDictVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String contrastId;
    private String colCode;
    private String colName;
    private String contrastGbId;
    private String contrastGbCode;
    private String contrastGbName;
    private String sourceName;
    private String tableName;
    private String columnName;
    private String gbTypeCode;
    private String gbTypeName;
}
