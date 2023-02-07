package cn.datax.service.data.visual.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>
 * 可视化看板配置信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
@ApiModel(value = "可视化看板配置信息表Model")
@Data
public class BoardDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "看板名称")
    @NotBlank(message = "看板名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String boardName;
    @ApiModelProperty(value = "看板缩略图(图片base64)")
    private String boardThumbnail;
    @ApiModelProperty(value = "看板配置")
    @Valid
    private BoardConfig boardConfig;
}
