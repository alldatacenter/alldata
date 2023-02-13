package cn.datax.service.data.market.api.vo;

import cn.datax.service.data.market.api.dto.FieldRule;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 数据API脱敏信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
public class ApiMaskVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String apiId;
    private String maskName;
    private List<FieldRule> rules;
}
