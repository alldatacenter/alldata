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

import datart.core.base.consts.FileOwner;
import datart.server.base.dto.ResponseData;
import datart.server.service.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Api
@RestController
@RequestMapping(value = "/files")
public class FileController extends BaseController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @ApiOperation(value = "upload viz background image")
    @PostMapping(value = "/viz/image")
    public ResponseData<String> uploadVizImage(@RequestParam FileOwner ownerType,
                                               @RequestParam String ownerId,
                                               @RequestParam(required = false) String fileName,
                                               @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseData.success(fileService.uploadFile(ownerType, ownerId, file, fileName));
    }

    @ApiOperation(value = "upload org avatar")
    @PostMapping(value = "/org/avatar")
    public ResponseData<String> uploadOrgAvatar(@RequestParam String orgId,
                                                @RequestParam(required = false) String fileName,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseData.success(fileService.uploadFile(FileOwner.ORG_AVATAR, orgId, file, fileName));
    }

    @ApiOperation(value = "delete org avatar")
    @DeleteMapping(value = "/org/avatar/{orgId}")
    public ResponseData<Boolean> deleteOrgAvatar(@PathVariable String orgId) {
        return ResponseData.success(fileService.deleteFiles(FileOwner.ORG_AVATAR, orgId));
    }

    @ApiOperation(value = "upload user avatar")
    @PostMapping(value = "/user/avatar")
    public ResponseData<String> uploadUserAvatar(@RequestParam String userId,
                                                 @RequestParam(required = false) String fileName,
                                                 @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseData.success(fileService.uploadFile(FileOwner.USER_AVATAR, userId, file, fileName));
    }

    @ApiOperation(value = "delete user avatar")
    @DeleteMapping(value = "/user/avatar/{userId}")
    public ResponseData<Boolean> deleteUserAvatar(@PathVariable String userId) {
        return ResponseData.success(fileService.deleteFiles(FileOwner.USER_AVATAR, userId));
    }

    @ApiOperation(value = "upload user avatar")
    @PostMapping(value = "/datasource")
    public ResponseData<String> uploadDatasourceFile(@RequestParam String sourceId,
                                                     @RequestParam("file") MultipartFile file) throws IOException {

        checkBlank(sourceId, "sourceId");
        return ResponseData.success(fileService.uploadFile(FileOwner.DATA_SOURCE, sourceId, file, null));
    }

}
