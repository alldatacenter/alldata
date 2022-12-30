package com.alibaba.tdata.aisp.server.common.condition;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: SceneQueryCondition
 * @Author: dyj
 * @DATE: 2021-11-18
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneQueryCondition {
    private String sceneCode;

    private String sceneCodeLike;

    private String owners;

    private List<String> productList;

    private String sceneName;

    private String sceneNameLike;
}
