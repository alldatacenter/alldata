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
 * @ClassName: SceneUpdateParam
 * @Author: dyj
 * @DATE: 2021-11-18
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "update场景")
public class SceneUpdateParam {
    @NotNull(message = "sceneCode can not be null!")
    @ApiModelProperty(notes = "场景Code", required = true)
    private String sceneCode;

    @ApiModelProperty(notes = "关联的产品")
    private List<String> productList;

    @NotNull(message = "ownerInfoList can not be null!")
    @ApiModelProperty(notes = "ownerInfoList")
    private List<UserSimpleInfo> ownerInfoList;

    private String sceneName;

    private String comment;
}
