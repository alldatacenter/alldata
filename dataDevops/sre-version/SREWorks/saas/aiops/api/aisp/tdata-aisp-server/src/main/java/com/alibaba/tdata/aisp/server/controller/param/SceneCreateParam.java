package com.alibaba.tdata.aisp.server.controller.param;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.alibaba.tdata.aisp.server.common.dto.UserSimpleInfo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: SceneCreateParam
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "scene create参数")
public class SceneCreateParam {
    @NotNull(message = "sceneCode can not be null!")
    @ApiModelProperty(notes = "场景Code", required = true)
    private String sceneCode;

    @NotNull(message = "sceneName can not be null!")
    @ApiModelProperty(notes = "场景名", required = true)
    private String sceneName;

    @ApiModelProperty(notes = "关联的产品")
    private List<String> productList;

    @NotNull(message = "ownerInfoList can not be null!")
    @ApiModelProperty(notes = "owner列表")
    private List<UserSimpleInfo> ownerInfoList;

    @ApiModelProperty(notes = "关联检测器")
    private String detectorBinder;

    @ApiModelProperty(notes = "备注")
    private String comment;
}
