package com.alibaba.tdata.aisp.server.common.dto;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: SolutionConfigDto
 * @Author: dyj
 * @DATE: 2022-05-09
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionConfigDto {
    private String algoInstanceCode;

    private Long gmtCreate;

    private Long gmtModified;

    private List<UserSimpleInfo> ownerInfoList;

    private String[] productList;

    private String algoInstanceName;

    private JSONObject instanceModelParam;

    private String detectorBinder;

    private String comment;

    public static SolutionConfigDto from(SceneConfigDto sceneConfigDto) {
        return SolutionConfigDto.builder()
            .algoInstanceCode(sceneConfigDto.getSceneCode())
            .algoInstanceName(sceneConfigDto.getSceneName())
            .productList(sceneConfigDto.getProductList())
            .ownerInfoList(sceneConfigDto.getOwnerInfoList())
            .instanceModelParam(sceneConfigDto.getSceneModelParam())
            .detectorBinder(sceneConfigDto.getDetectorBinder())
            .comment(sceneConfigDto.getComment())
            .build();
    }

    public static List<SolutionConfigDto> froms(List<SceneConfigDto> sceneConfigDtoList) {
        return sceneConfigDtoList.stream().map(SolutionConfigDto::from)
            .collect(Collectors.toList());
    }
}
