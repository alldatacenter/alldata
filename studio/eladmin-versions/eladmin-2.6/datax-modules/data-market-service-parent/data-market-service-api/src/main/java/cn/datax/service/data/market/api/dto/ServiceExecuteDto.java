package cn.datax.service.data.market.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(value = "服务调用Model")
@Data
public class ServiceExecuteDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "服务编号")
    private String serviceNo;

    @ApiModelProperty(value = "http服务请求头")
    private String header;
    @ApiModelProperty(value = "http服务请求参数")
    private String param;

    @ApiModelProperty(value = "webservice服务请求报文")
    private String soap;
}
