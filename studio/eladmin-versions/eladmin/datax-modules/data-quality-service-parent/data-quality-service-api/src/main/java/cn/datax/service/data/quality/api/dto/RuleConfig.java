package cn.datax.service.data.quality.api.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class RuleConfig implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "核查类型编码")
    private String ruleItemCode;

    /**
     * 一致性
     */
    private Consistent consistent;

    /**
     * 关联性
     */
    private Relevance relevance;

    /**
     * 及时性
     */
    private Timeliness timeliness;

    /**
     * 准确性
     */
    private Accuracy accuracy;
}
