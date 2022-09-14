package com.alibaba.tesla.tkgone.backend.controller.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 存储后端后端VO
 * @author xueyong.zxy
 */
@ApiModel(description = "存储后端信息")
@Data
public class BackendVO {

    @ApiModelProperty(value = "存储后端类型", required = true, position = 10, example = "ElasticSearch")
    @NotBlank
    private String type;

    @ApiModelProperty(value = "存储后端名称", required = true, position = 20, example = "xxx's es cluster")
    @NotBlank
    private String name;

    @ApiModelProperty(value = "存储后端IP地址", required = true, position = 30, example = "100.96.1.1")
    @NotBlank
    private String host;

    @ApiModelProperty(value = "存储后端端口号", allowableValues = "@code range(1024, 65535)",
            position = 40, example = "9200")
    private int port;

    @ApiModelProperty(value = "存储后端用户名", position = 50, example = "xxx")
    private String user;

    @ApiModelProperty(value = "存储后端密码", position = 60, example = "123456")
    private String password;
}
