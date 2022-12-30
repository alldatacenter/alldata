package cn.datax.service.data.market.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(value = "http接口Model")
@Data
public class HttpService implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "请求地址")
    private String url;
    @ApiModelProperty(value = "请求头")
    private String header;
    @ApiModelProperty(value = "请求参数")
    private String param;
    @ApiModelProperty(value = "请求方式")
    private String httpMethod;
}
