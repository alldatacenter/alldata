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
import datart.server.service.CustomPluginService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.util.Set;

@Api
@RestController
@RequestMapping(value = "/plugins")
public class CustomPluginController extends BaseController {

    private final CustomPluginService customPluginService;

    public CustomPluginController(CustomPluginService customPluginService) {
        this.customPluginService = customPluginService;
    }

    @SkipLogin
    @ApiOperation(value = "scan custom chart plugins")
    @GetMapping(value = "/custom/charts")
    public ResponseData<Set<String>> scanCustomCharts() throws MalformedURLException {
        return ResponseData.success(customPluginService.scanCustomChartPlugins());
    }

}
