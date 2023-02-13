package cn.datax.service.system.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统参数配置信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @since 2023-01-19
 */
@Data
public class ConfigVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String configName;
    private String configKey;
    private String configValue;
}
