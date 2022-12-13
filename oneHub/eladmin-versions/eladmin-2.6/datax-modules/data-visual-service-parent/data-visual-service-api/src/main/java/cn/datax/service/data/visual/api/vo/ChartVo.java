package cn.datax.service.data.visual.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 可视化图表配置信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
@Data
public class ChartVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String chartName;
    private String chartThumbnail;
    private String chartConfig;
}
