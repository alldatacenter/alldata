package cn.datax.service.data.market.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@ApiModel(value = "执行配置信息Model")
@Data
public class ExecuteConfig implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "数据源")
    @NotBlank(message = "数据源不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String sourceId;

    @ApiModelProperty(value = "配置方式")
    @NotNull(message = "配置方式不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String configType;

    @ApiModelProperty(value = "数据库表主键")
    private String tableId;

    @ApiModelProperty(value = "数据库表")
    private String tableName;

    @ApiModelProperty(value = "表字段列表")
    @Valid
    private List<FieldParam> fieldParams;

    @ApiModelProperty(value = "解析SQL")
    private String sqlText;
}
