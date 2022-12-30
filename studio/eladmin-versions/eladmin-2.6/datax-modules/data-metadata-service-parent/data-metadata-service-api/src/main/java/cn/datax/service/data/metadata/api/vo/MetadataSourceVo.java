package cn.datax.service.data.metadata.api.vo;

import cn.datax.service.data.metadata.api.dto.DbSchema;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 数据源信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
public class MetadataSourceVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String dbType;
    private String sourceName;
    private DbSchema dbSchema;
    private String isSync;
}
