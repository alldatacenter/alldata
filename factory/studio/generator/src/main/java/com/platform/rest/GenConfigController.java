
package com.platform.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import com.platform.domain.GenConfig;
import com.platform.service.GenConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/genConfig")
@Api(tags = "系统：代码生成器配置管理")
public class GenConfigController {

    private final GenConfigService genConfigService;

    @ApiOperation("查询")
    @GetMapping(value = "/{tableName}")
    public ResponseEntity<Object> queryGenConfig(@PathVariable String tableName){
        return new ResponseEntity<>(genConfigService.find(tableName), HttpStatus.OK);
    }

    @PutMapping
    @ApiOperation("修改")
    public ResponseEntity<Object> updateGenConfig(@Validated @RequestBody GenConfig genConfig){
        return new ResponseEntity<>(genConfigService.update(genConfig.getTableName(), genConfig),HttpStatus.OK);
    }
}
