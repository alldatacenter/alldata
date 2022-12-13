package cn.datax.service.data.visual.api.vo;

import cn.datax.service.data.visual.api.dto.SchemaConfig;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 数据集信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Data
public class DataSetVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String sourceId;
    private String setName;
    private String setSql;
    private SchemaConfig schemaConfig;
}
