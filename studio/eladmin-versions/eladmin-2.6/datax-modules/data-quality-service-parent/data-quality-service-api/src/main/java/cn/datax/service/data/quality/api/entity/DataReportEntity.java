package cn.datax.service.data.quality.api.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class DataReportEntity implements Serializable {

    private static final long serialVersionUID=1L;

    private String ruleTypeId;
    private String ruleTypeName;
    private String ruleId;
    private String ruleName;
    private String ruleSourceId;
    private String ruleSourceName;
    private String ruleLevelId;
    private String ruleLevelName;
    private Integer checkErrorCount;

    private String ruleTypeCode;
    private String ruleTableName;
    private String ruleTableComment;
    private String ruleColumnName;
    private String ruleColumnComment;
    private Integer checkTotalCount;
}
