package cn.datax.service.data.standard.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>
 * 对照表信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@ApiModel(value = "对照表信息表Model")
@Data
public class ContrastDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "数据源主键")
    private String sourceId;
    @ApiModelProperty(value = "数据源")
    private String sourceName;
    @ApiModelProperty(value = "数据表主键")
    private String tableId;
    @ApiModelProperty(value = "数据表")
    private String tableName;
    @ApiModelProperty(value = "数据表名称")
    private String tableComment;
    @ApiModelProperty(value = "对照字段主键")
    private String columnId;
    @ApiModelProperty(value = "对照字段")
    private String columnName;
    @ApiModelProperty(value = "对照字段名称")
    private String columnComment;
    @ApiModelProperty(value = "标准类别主键")
    private String gbTypeId;
    @ApiModelProperty(value = "绑定标准字段")
    private String bindGbColumn;
}
