
package com.platform.modules.system.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import com.platform.modules.system.service.MonitorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "系统-服务监控管理")
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService serverService;

    @GetMapping
    @ApiOperation("查询服务监控")
    @PreAuthorize("@el.check('monitor:list')")
    public ResponseEntity<Object> queryMonitor(){
        return new ResponseEntity<>(serverService.getServers(),HttpStatus.OK);
    }
}
