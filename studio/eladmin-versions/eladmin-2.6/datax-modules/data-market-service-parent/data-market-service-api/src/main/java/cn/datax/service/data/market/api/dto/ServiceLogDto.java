package cn.datax.service.data.market.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务集成调用日志表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@ApiModel(value = "服务集成调用日志表Model")
@Data
public class ServiceLogDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "服务id")
    private String serviceId;
    @ApiModelProperty(value = "调用者id")
    private String callerId;
    @ApiModelProperty(value = "调用者ip")
    private String callerIp;
    @ApiModelProperty(value = "调用时间")
    private LocalDateTime callerDate;
    @ApiModelProperty(value = "调用请求头")
    private String callerHeader;
    @ApiModelProperty(value = "调用请求参数")
    private String callerParam;
    @ApiModelProperty(value = "调用报文")
    private String callerSoap;
    @ApiModelProperty(value = "调用耗时")
    private Long time;
    @ApiModelProperty(value = "信息记录")
    private String msg;
    @ApiModelProperty(value = "状态：0:失败，1：成功")
    private String status;
}
