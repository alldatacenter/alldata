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


import datart.core.data.provider.SchemaInfo;
import datart.core.entity.Source;
import datart.server.base.dto.ResponseData;
import datart.server.base.params.*;
import datart.server.service.SourceService;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/sources")
public class SourceController extends BaseController {

    private final SourceService sourceService;

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @ApiOperation(value = "check source name is unique")
    @PostMapping("/check/name")
    public ResponseData<Boolean> checkSourceName(@Validated @RequestBody CheckNameParam param) {
        return ResponseData.success(sourceService.checkUnique(param.getOrgId(), param.getParentId(), param.getName()));
    }

    @ApiOperation(value = "Get Org available sources")
    @GetMapping
    public ResponseData<List<Source>> listOrgSources(@RequestParam String orgId) {
        checkBlank(orgId, "orgId");
        return ResponseData.success(sourceService.listSources(orgId, true));
    }

    @ApiOperation(value = "get source detail")
    @GetMapping("/{sourceId}")
    public ResponseData<Source> getSourceDetail(@PathVariable String sourceId) {
        checkBlank(sourceId, "sourceId");
        return ResponseData.success(sourceService.retrieve(sourceId));
    }

    @ApiOperation(value = "create source")
    @PostMapping()
    public ResponseData<Source> createSource(@Validated @RequestBody SourceCreateParam createParam) {
        return ResponseData.success(sourceService.createSource(createParam));
    }

    @ApiOperation(value = "update a source")
    @PutMapping(value = "/{sourceId}")
    public ResponseData<Boolean> updateSource(@PathVariable String sourceId,
                                              @Validated @RequestBody SourceUpdateParam updateParam) {
        return ResponseData.success(sourceService.updateSource(updateParam));
    }

    @ApiOperation(value = "update a source base info")
    @PutMapping(value = "/{sourceId}/base")
    public ResponseData<Boolean> updateSourceBaseInfo(@PathVariable String sourceId,
                                                      @Validated @RequestBody SourceBaseUpdateParam updateParam) {
        checkBlank(sourceId, "sourceId");
        return ResponseData.success(sourceService.updateBase(updateParam));
    }

    @ApiOperation(value = "delete a source")
    @DeleteMapping("/{sourceId}")
    public ResponseData<Boolean> deleteSource(@PathVariable String sourceId,
                                              @RequestParam boolean archive) {
        checkBlank(sourceId, "sourceId");
        return ResponseData.success(sourceService.delete(sourceId, archive, true));
    }

    @ApiOperation(value = "list archived source")
    @GetMapping(value = "/archived")
    public ResponseData<List<Source>> listArchived(@RequestParam String orgId) {
        return ResponseData.success(sourceService.listSources(orgId, false));
    }

    @ApiOperation(value = "unarchive a source")
    @PutMapping(value = "/unarchive/{sourceId}")
    public ResponseData<Boolean> unarchive(@PathVariable String sourceId,
                                           @RequestParam String name,
                                           @RequestParam Double index,
                                           @RequestParam(required = false) String parentId) {
        return ResponseData.success(sourceService.unarchive(sourceId, name, parentId, index));
    }

    @ApiOperation(value = "get source schemas ")
    @GetMapping(value = "/schemas/{sourceId}")
    public ResponseData<SchemaInfo> getSourceSchemas(@PathVariable String sourceId) {
        return ResponseData.success(sourceService.getSourceSchemaInfo(sourceId));
    }

    @ApiOperation(value = "sync source schemas ")
    @GetMapping(value = "/sync/schemas/{sourceId}")
    public ResponseData<SchemaInfo> syncSourceSchemas(@PathVariable String sourceId) throws Exception {
        return ResponseData.success(sourceService.syncSourceSchema(sourceId));
    }

}