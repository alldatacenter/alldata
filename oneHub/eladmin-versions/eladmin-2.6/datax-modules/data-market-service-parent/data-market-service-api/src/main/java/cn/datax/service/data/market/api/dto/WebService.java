package cn.datax.service.data.market.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(value = "webservice接口Model")
@Data
public class WebService implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "wsdl地址")
    private String wsdl;
    @ApiModelProperty(value = "命名空间")
    private String targetNamespace;
    @ApiModelProperty(value = "请求报文")
    private String soap;
    @ApiModelProperty(value = "调用方法")
    private String method;
}
