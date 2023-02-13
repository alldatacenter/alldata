package cn.datax.service.data.quality.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 规则级别信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
public class RuleLevelVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String code;
    private String name;
}
