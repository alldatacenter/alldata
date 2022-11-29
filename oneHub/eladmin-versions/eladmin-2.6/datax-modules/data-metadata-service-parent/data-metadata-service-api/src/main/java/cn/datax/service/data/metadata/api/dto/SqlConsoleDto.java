package cn.datax.service.data.metadata.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class SqlConsoleDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "当前时间戳")
    @NotBlank(message = "时间戳不能为空", groups = {ValidationGroups.Other.class})
    private String sqlKey;

    @ApiModelProperty(value = "数据源")
    @NotBlank(message = "数据源不能为空")
    private String sourceId;

    @ApiModelProperty(value = "SQL文本")
    @NotBlank(message = "SQL不能为空")
    private String sqlText;
}
