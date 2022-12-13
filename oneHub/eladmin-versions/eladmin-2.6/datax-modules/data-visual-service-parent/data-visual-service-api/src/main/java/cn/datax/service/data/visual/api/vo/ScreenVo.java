package cn.datax.service.data.visual.api.vo;

import cn.datax.service.data.visual.api.dto.ScreenConfig;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 可视化酷屏配置信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Data
public class ScreenVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String screenName;
    private String screenThumbnail;
    private ScreenConfig screenConfig;
    private List<ChartVo> charts;
}
