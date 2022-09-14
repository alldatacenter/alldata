package com.alibaba.tdata.aisp.server.common.dto;

import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: SceneConfigDto
 * @Author: dyj
 * @DATE: 2021-11-18
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneConfigDto {
    private String sceneCode;

    private Long gmtCreate;

    private Long gmtModified;

    private List<UserSimpleInfo> ownerInfoList;

    private String[] productList;

    private String sceneName;

    private JSONObject sceneModelParam;

    private String detectorBinder;

    private String comment;
}
