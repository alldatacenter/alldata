package com.alibaba.tdata.aisp.server.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.tdata.aisp.server.common.constant.AispResult;
import com.alibaba.tdata.aisp.server.common.dto.SceneConfigDto;
import com.alibaba.tdata.aisp.server.common.dto.SolutionConfigDto;
import com.alibaba.tdata.aisp.server.controller.param.SceneCleanModelParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneUpsertModelParam;
import com.alibaba.tdata.aisp.server.controller.param.SolutionCleanModelParam;
import com.alibaba.tdata.aisp.server.controller.param.SolutionCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.SolutionQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.SolutionUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.SolutionUpsertModelParam;
import com.alibaba.tdata.aisp.server.service.SceneService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildSuccessResult;

/**
 * @ClassName: AiSolutionController
 * @Author: dyj
 * @DATE: 2022-05-09
 * @Description:
 **/
@Api(tags = "解决方案接口")
@Slf4j
@RestController
@RequestMapping("/solution/")
public class AiSolutionController {
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private SceneService sceneService;

    @ApiOperation("新建")
    @PostMapping(value = "create")
    @ResponseBody
    public AispResult create(@RequestBody @Validated SolutionCreateParam param){
        SceneCreateParam createParam = SceneCreateParam.builder()
            .sceneCode(param.getAlgoInstanceCode())
            .sceneName(param.getAlgoInstanceName())
            .productList(param.getProductList())
            .ownerInfoList(param.getOwnerInfoList())
            .comment(param.getComment())
            .detectorBinder(param.getDetectorBinder())
            .build();
        return buildSuccessResult(sceneService.create(createParam));
    }

    @ApiOperation("查询")
    @PostMapping(value = "list")
    @ResponseBody
    public AispResult list(@RequestBody @Validated SolutionQueryParam param){
        SceneQueryParam queryParam = SceneQueryParam.builder().sceneCodeLike(param.getAlgoInstanceCode())
            .sceneNameLike(param.getAlgoInstanceName())
            .owners(param.getOwners())
            .productList(param.getProductList())
            .page(param.getPage())
            .pageSize(param.getPageSize())
            .build();
        List<SceneConfigDto> sceneConfigDtos = sceneService.list(queryParam, getUserEmployeeId());
        return buildSuccessResult(SolutionConfigDto.froms(sceneConfigDtos));
    }

    @ApiOperation("更新")
    @PostMapping(value = "update")
    @ResponseBody
    public AispResult list(@RequestBody @Validated SolutionUpdateParam param){
        SceneUpdateParam updateParam = SceneUpdateParam.builder().sceneCode(param.getAlgoInstanceCode())
            .sceneName(param.getAlgoInstanceName())
            .ownerInfoList(param.getOwnerInfoList())
            .productList(param.getProductList())
            .comment(param.getComment())
            .build();
        return buildSuccessResult(sceneService.update(updateParam));
    }

    @ApiOperation("更新modelParam")
    @PostMapping(value = "upsert")
    @ResponseBody
    public AispResult upsertModel(@RequestBody @Validated SolutionUpsertModelParam param){
        SceneUpsertModelParam upsertModelParam = SceneUpsertModelParam.builder().sceneCode(param.getAlgoInstanceCode())
            .detectorCode(param.getDetectorCode())
            .sceneModelParam(param.getInstanceModelParam())
            .build();
        return buildSuccessResult(sceneService.upsertModel(upsertModelParam));
    }

    @ApiOperation("删除modelParam")
    @PostMapping(value = "cleanModel")
    @ResponseBody
    public AispResult cleanModel(@RequestBody @Validated SolutionCleanModelParam param){
        SceneCleanModelParam cleanModelParam = SceneCleanModelParam.builder().sceneCode(param.getAlgoInstanceCode())
            .detectorCode(param.getDetectorCode())
            .build();
        return buildSuccessResult(sceneService.cleanModel(cleanModelParam));
    }

    @ApiOperation("删除")
    @DeleteMapping(value = "delete")
    @ResponseBody
    public AispResult delete(@RequestParam(name = "algoInstanceCode") String algoInstanceCode){
        return buildSuccessResult(sceneService.delete(algoInstanceCode));
    }

    public String getUserEmployeeId() {
        String employeeId = httpServletRequest.getHeader("x-empid");
        employeeId = employeeId == null ? "" : employeeId;
        return employeeId;
    }
}
