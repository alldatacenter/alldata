package cn.datax.service.data.quality.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 数据质量监控任务日志信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-13
 */
@ApiModel(value = "数据质量监控任务日志信息表Model")
@Data
public class ScheduleLogDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "状态（1成功 0失败）")
    private String status;
    @ApiModelProperty(value = "执行任务主键")
    private String executeJobId;
    @ApiModelProperty(value = "执行规则主键")
    private String executeRuleId;
    @ApiModelProperty(value = "执行时间")
    private LocalDateTime executeDate;
    @ApiModelProperty(value = "执行结果")
    private String executeResult;
    @ApiModelProperty(value = "执行批次号")
    private String executeBatch;
}
