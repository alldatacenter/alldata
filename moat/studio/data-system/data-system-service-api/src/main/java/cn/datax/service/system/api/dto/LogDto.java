package cn.datax.service.system.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@ApiModel(value = "日志Model")
@Data
@Accessors(chain = true)
public class LogDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    private String id;

    @ApiModelProperty(value = "所属模块")
    private String module;

    @ApiModelProperty(value = "日志标题")
    private String title;

    @ApiModelProperty(value = "用户ID")
    private String userId;

    @ApiModelProperty(value = "用户名称")
    private String userName;

    @ApiModelProperty(value = "请求IP")
    private String remoteAddr;

    @ApiModelProperty(value = "请求URI")
    private String requestUri;

    @ApiModelProperty(value = "方法类名")
    private String className;

    @ApiModelProperty(value = "方法名称")
    private String methodName;

    @ApiModelProperty(value = "请求参数")
    private String params;

    @ApiModelProperty(value = "请求耗时")
    private String time;

    @ApiModelProperty(value = "浏览器名称")
    private String browser;

    @ApiModelProperty(value = "操作系统")
    private String os;

    @ApiModelProperty(value = "错误类型")
    private String exCode;

    @ApiModelProperty(value = "错误信息")
    private String exMsg;
}
