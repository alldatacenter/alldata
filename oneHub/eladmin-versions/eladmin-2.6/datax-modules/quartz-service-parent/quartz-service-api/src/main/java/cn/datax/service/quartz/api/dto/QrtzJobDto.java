package cn.datax.service.quartz.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
/**
 * <p>
 * 定时任务信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@ApiModel(value = "定时任务信息表Model")
@Data
public class QrtzJobDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "任务名称")
    @NotBlank(message = "任务名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String jobName;
    @ApiModelProperty(value = "Spring Bean名称")
    @NotBlank(message = "Spring Bean名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String beanName;
    @ApiModelProperty(value = "方法名称")
    @NotBlank(message = "方法名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String methodName;
    @ApiModelProperty(value = "方法参数")
    private String methodParams;
    @ApiModelProperty(value = "cron表达式")
    @NotBlank(message = "cron表达式不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String cronExpression;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
