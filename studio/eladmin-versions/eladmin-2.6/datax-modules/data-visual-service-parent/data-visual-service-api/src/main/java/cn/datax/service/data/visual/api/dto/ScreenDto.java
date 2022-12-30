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
 * 可视化酷屏配置信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@ApiModel(value = "可视化酷屏配置信息表Model")
@Data
public class ScreenDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "酷屏名称")
    private String screenName;
    @ApiModelProperty(value = "酷屏缩略图(图片base64)")
    private String screenThumbnail;
    @ApiModelProperty(value = "酷屏配置")
    @Valid
    private ScreenConfig screenConfig;
}
