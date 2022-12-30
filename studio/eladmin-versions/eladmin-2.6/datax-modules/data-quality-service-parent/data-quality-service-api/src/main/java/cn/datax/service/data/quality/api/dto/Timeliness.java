package cn.datax.service.data.quality.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 及时性
 */
@Data
public class Timeliness implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 判定阀值 当前时间-业务时间>阀值
     */
    private Integer threshold;
}
