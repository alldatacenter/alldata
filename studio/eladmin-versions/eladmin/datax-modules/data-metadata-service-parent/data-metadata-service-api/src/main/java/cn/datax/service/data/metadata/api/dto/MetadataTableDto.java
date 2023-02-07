package cn.datax.service.data.metadata.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>
 * 数据库表信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@ApiModel(value = "数据库表信息表Model")
@Data
public class MetadataTableDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "所属数据源")
    private String sourceId;
    @ApiModelProperty(value = "表名")
    private String tableName;
    @ApiModelProperty(value = "表注释")
    private String tableComment;
}
