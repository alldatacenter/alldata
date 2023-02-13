package cn.datax.service.data.quality.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 核查报告信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
public class CheckReportVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String checkRuleId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime checkDate;
    private String checkResult;
    private Integer checkTotalCount;
    private Integer checkErrorCount;
    private String ruleName;
    private String ruleType;
    private String ruleSource;
    private String ruleTable;
    private String ruleColumn;
}
