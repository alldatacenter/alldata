package com.hw.lineage.server.interfaces.controller;

import com.hw.lineage.common.result.FunctionResult;
import com.hw.lineage.server.application.command.function.ParseFunctionCmd;
import com.hw.lineage.server.application.service.FunctionService;
import com.hw.lineage.server.interfaces.result.Result;
import com.hw.lineage.server.interfaces.result.ResultMessage;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * @description: FunctionController
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Validated
@RestController
@Api(tags = "Functions API")
@RequestMapping("/functions")
public class FunctionController {

    @Resource
    private FunctionService functionService;

    @PostMapping("/parse")
    public Result<List<FunctionResult>> parseFunction(@Valid @RequestBody ParseFunctionCmd command)
            throws IOException, ClassNotFoundException {
        List<FunctionResult> functionDTOList = functionService.parseFunction(command);
        return Result.success(ResultMessage.PARSE_FUNCTION_SUCCESS, functionDTOList);
    }
}
