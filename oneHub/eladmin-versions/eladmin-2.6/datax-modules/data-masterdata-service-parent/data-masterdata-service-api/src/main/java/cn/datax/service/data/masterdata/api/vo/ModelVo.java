package cn.datax.service.data.masterdata.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 主数据模型表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Data
public class ModelVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String modelName;
    private String modelLogicTable;
    private String modelPhysicalTable;
    private String isSync;
    private String flowStatus;
    private String processInstanceId;
    private List<ModelColumnVo> modelColumns;
}
