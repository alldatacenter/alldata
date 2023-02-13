package cn.datax.service.data.quality.api.entity;

import lombok.Data;

@Data
public class RuleTypeReportEntity extends RuleTypeEntity {

    private static final long serialVersionUID=1L;

    /**
     * 不合规数量
     */
    private Integer checkErrorCount;
}
