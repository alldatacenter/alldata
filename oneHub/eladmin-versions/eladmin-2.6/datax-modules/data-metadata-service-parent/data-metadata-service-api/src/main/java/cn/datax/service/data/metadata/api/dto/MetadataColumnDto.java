package cn.datax.service.data.metadata.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>
 * 元数据信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@ApiModel(value = "元数据信息表Model")
@Data
public class MetadataColumnDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "所属数据源")
    private String sourceId;
    @ApiModelProperty(value = "所属数据表")
    private String tableId;
    @ApiModelProperty(value = "字段名称")
    private String columnName;
    @ApiModelProperty(value = "字段注释")
    private String columnComment;
    @ApiModelProperty(value = "字段是否主键(1是0否)")
    private String columnKey;
    @ApiModelProperty(value = "字段是否允许为空(1是0否)")
    private String columnNullable;
    @ApiModelProperty(value = "字段序号")
    private String columnPosition;
    @ApiModelProperty(value = "数据类型")
    private String dataType;
    @ApiModelProperty(value = "数据长度")
    private String dataLength;
    @ApiModelProperty(value = "数据精度")
    private String dataPrecision;
    @ApiModelProperty(value = "数据小数位")
    private String dataScale;
    @ApiModelProperty(value = "数据默认值")
    private String dataDefault;
}
