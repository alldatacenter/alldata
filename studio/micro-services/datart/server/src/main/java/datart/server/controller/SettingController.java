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

import datart.core.entity.OrgSettings;
import datart.core.entity.UserSettings;
import datart.server.base.dto.ResponseData;
import datart.server.base.params.OrgSettingsUpdateParam;
import datart.server.base.params.OrgSettingsCreateParam;
import datart.server.base.params.UserSettingsCreateParam;
import datart.server.base.params.UserSettingsUpdateParam;
import datart.server.service.OrgSettingService;
import datart.server.service.UserSettingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = SettingController.PER_PATH)
@Slf4j
@RestController
@RequestMapping(value = SettingController.PER_PATH)
public class SettingController extends BaseController {

    public static final String PER_PATH = "/settings";

    private final OrgSettingService orgSettingService;

    private final UserSettingService userSettingService;

    public SettingController(OrgSettingService orgSettingService, UserSettingService userSettingService) {
        this.orgSettingService = orgSettingService;
        this.userSettingService = userSettingService;
    }

    @ApiOperation(value = "list user settings")
    @GetMapping(value = "/user")
    public ResponseData<List<UserSettings>> listUserSettings() {
        return ResponseData.success(userSettingService.listUserSettings());
    }

    @ApiOperation(value = "create a user setting")
    @PostMapping(value = "/user")
    public ResponseData<UserSettings> createUserSetting(@Validated @RequestBody UserSettingsCreateParam createParam) {
        return ResponseData.success(userSettingService.create(createParam));
    }

    @ApiOperation(value = "update a user setting")
    @PutMapping(value = "/user/{id}")
    public ResponseData<Boolean> updateUserSetting(@PathVariable String id, @Validated @RequestBody UserSettingsUpdateParam updateParam) {
        return ResponseData.success(userSettingService.update(updateParam));
    }

    @ApiOperation(value = "delete a user setting")
    @DeleteMapping(value = "/user/{id}")
    public ResponseData<Boolean> deleteUserSetting(@PathVariable String id) {
        return ResponseData.success(userSettingService.delete(id));
    }

    @ApiOperation(value = "list org settings")
    @GetMapping(value = "/org/{orgId}")
    public ResponseData<List<OrgSettings>> listOrgSettings(@PathVariable String orgId) {
        checkBlank(orgId, "orgId");
        return ResponseData.success(orgSettingService.listOrgSettings(orgId));
    }

    @ApiOperation(value = "create a org setting")
    @PostMapping(value = "/org")
    public ResponseData<OrgSettings> createOrgSetting(@Validated @RequestBody OrgSettingsCreateParam createParam) {
        return ResponseData.success(orgSettingService.create(createParam));
    }

    @ApiOperation(value = "update a org setting")
    @PutMapping(value = "/org/{id}")
    public ResponseData<Boolean> updateOrgSetting(@PathVariable String id, @Validated @RequestBody OrgSettingsUpdateParam updateParam) {
        return ResponseData.success(orgSettingService.update(updateParam));
    }

    @ApiOperation(value = "delete a org setting")
    @DeleteMapping(value = "/org/{id}")
    public ResponseData<Boolean> deleteOrgSetting(@PathVariable String id) {
        checkBlank(id, "id");
        return ResponseData.success(orgSettingService.delete(id));
    }

}