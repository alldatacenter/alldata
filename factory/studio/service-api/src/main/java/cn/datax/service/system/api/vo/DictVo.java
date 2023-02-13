package cn.datax.service.system.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 字典编码信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @since 2023-01-17
 */
@Data
public class DictVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String dictName;
    private String dictCode;
}
