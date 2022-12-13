package cn.datax.service.quartz.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
/**
 * <p>
 * 定时任务日志信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@ApiModel(value = "定时任务日志信息表Model")
@Data
public class QrtzJobLogDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "任务ID")
    @NotBlank(message = "任务ID不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String jobId;
    @ApiModelProperty(value = "信息记录")
    @NotBlank(message = "信息记录不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String msg;
}
