package cn.datax.service.system.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 登录日志信息表 实体DTO
 * </p>
 *
 * @author yuwei
 * @date 2022-05-29
 */
@ApiModel(value = "登录日志信息表Model")
@Data
public class LoginLogDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    private String id;
    @ApiModelProperty(value = "操作系统")
    private String opOs;
    @ApiModelProperty(value = "浏览器类型")
    private String opBrowser;
    @ApiModelProperty(value = "登录IP地址")
    private String opIp;
    @ApiModelProperty(value = "登录时间")
    private LocalDateTime opDate;
    @ApiModelProperty(value = "登录用户ID")
    private String userId;
    @ApiModelProperty(value = "登录用户名称")
    private String userName;
}
