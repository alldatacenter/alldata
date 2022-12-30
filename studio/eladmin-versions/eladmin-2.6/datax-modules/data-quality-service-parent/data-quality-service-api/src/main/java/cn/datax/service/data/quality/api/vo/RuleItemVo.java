package cn.datax.service.data.quality.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 规则核查项信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Data
public class RuleItemVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String ruleTypeId;
    private String itemCode;
    private String itemExplain;
}
