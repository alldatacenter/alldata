package cn.datax.service.data.market.api.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class SqlParseDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "数据源")
    @NotBlank(message = "数据源不能为空")
    private String sourceId;

    @ApiModelProperty(value = "SQL文本")
    @NotBlank(message = "SQL不能为空")
    private String sqlText;
}
