/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.controller;

import datart.core.base.annotations.SkipLogin;
import datart.server.base.dto.ResponseData;
import datart.server.base.dto.SystemInfo;
import datart.server.base.params.SetupParams;
import datart.server.service.SysService;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping(value = "/sys")
public class SysController extends BaseController {

    private final SysService sysService;

    public SysController(SysService sysService) {
        this.sysService = sysService;
    }

    @SkipLogin
    @ApiOperation(value = "get system info")
    @GetMapping("/info")
    public ResponseData<SystemInfo> getSysInfo() {
        return ResponseData.success(sysService.getSysInfo());
    }

    @SkipLogin
    @ApiOperation(value = "initialized user info")
    @PostMapping("/setup")
    public ResponseData<Boolean> setup(@Validated @RequestBody SetupParams setupParams) throws MessagingException, UnsupportedEncodingException {
        return ResponseData.success(sysService.setup(setupParams));
    }

}
