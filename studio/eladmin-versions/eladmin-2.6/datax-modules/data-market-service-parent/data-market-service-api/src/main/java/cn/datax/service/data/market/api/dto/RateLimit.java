package cn.datax.service.data.market.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel(value = "限流信息Model")
@Data
public class RateLimit implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "是否限流：0:否，1：是")
    @NotNull(message = "是否限流不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String enable;
    @ApiModelProperty(value = "请求次数默认5次")
    private Integer times = 5;
    @ApiModelProperty(value = "请求时间范围默认60秒")
    private Integer seconds = 60;
}
