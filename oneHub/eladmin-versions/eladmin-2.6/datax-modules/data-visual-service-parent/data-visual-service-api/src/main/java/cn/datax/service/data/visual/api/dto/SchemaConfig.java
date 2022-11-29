package cn.datax.service.data.visual.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@ApiModel(value = "数据模型定义")
@Data
public class SchemaConfig implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "解析字段")
    private List<String> columns;

    @ApiModelProperty(value = "维度列")
    @NotEmpty(message = "维度列不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, message="维度列长度不能少于{min}位")
    private List<ColumnParse> dimensions;

    @ApiModelProperty(value = "指标列")
    @NotEmpty(message = "指标列不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, message="指标列长度不能少于{min}位")
    private List<ColumnParse> measures;
}
