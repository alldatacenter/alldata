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

import datart.core.entity.View;
import datart.server.base.dto.ResponseData;
import datart.server.base.dto.ViewDetailDTO;
import datart.server.base.params.CheckNameParam;
import datart.server.base.params.ViewBaseUpdateParam;
import datart.server.base.params.ViewCreateParam;
import datart.server.base.params.ViewUpdateParam;
import datart.server.service.ViewService;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/views")
public class ViewController extends BaseController {

    private final ViewService viewService;

    public ViewController(ViewService viewService) {
        this.viewService = viewService;
    }

    @ApiOperation(value = "check view name is unique")
    @PostMapping("/check/name")
    public ResponseData<Boolean> checkViewName(@Validated @RequestBody CheckNameParam param) {
        return ResponseData.success(viewService.checkUnique(param.getOrgId(), param.getParentId(), param.getName()));
    }

    @ApiOperation(value = "get org views")
    @GetMapping
    public ResponseData<List<View>> listViews(@RequestParam String orgId) {
        checkBlank(orgId, "orgId");
        return ResponseData.success(viewService.getViews(orgId));
    }

    @ApiOperation(value = "get view detail")
    @GetMapping("/{viewId}")
    public ResponseData<ViewDetailDTO> getViewDetail(@PathVariable String viewId) {
        checkBlank(viewId, "viewId");
        return ResponseData.success(viewService.getViewDetail(viewId));
    }

    @ApiOperation(value = "create view")
    @PostMapping()
    public ResponseData<View> createView(@Validated @RequestBody ViewCreateParam createParam) {
        return ResponseData.success(viewService.create(createParam));
    }

    @ApiOperation(value = "update a view")
    @PutMapping(value = "/{viewId}")
    public ResponseData<View> updateView(@PathVariable String viewId,
                                         @Validated @RequestBody ViewUpdateParam updateParam) {
        checkBlank(viewId, "viewId");
        updateParam.setId(viewId);
        return ResponseData.success(viewService.updateView(updateParam));
    }

    @ApiOperation(value = "update a view base info")
    @PutMapping(value = "/{viewId}/base")
    public ResponseData<Boolean> updateViewBaseInfo(@PathVariable String viewId,
                                                    @Validated @RequestBody ViewBaseUpdateParam updateParam) {
        checkBlank(viewId, "viewId");
        return ResponseData.success(viewService.updateBase(updateParam));
    }

    @ApiOperation(value = "delete a view")
    @DeleteMapping("/{viewId}")
    public ResponseData<Boolean> deleteView(@PathVariable String viewId,
                                            @RequestParam boolean archive) {
        checkBlank(viewId, "viewId");
        return ResponseData.success(viewService.delete(viewId, archive));

    }

    @ApiOperation(value = "list archived source")
    @GetMapping(value = "/archived")
    public ResponseData<List<View>> listArchived(@RequestParam String orgId) {
        return ResponseData.success(viewService.listArchived(orgId));
    }

    @ApiOperation(value = "unarchive a source")
    @PutMapping(value = "/unarchive/{viewId}")
    public ResponseData<Boolean> unarchive(@PathVariable String viewId,
                                           @RequestParam String name,
                                           @RequestParam Double index,
                                           @RequestParam(required = false) String parentId) {
        return ResponseData.success(viewService.unarchive(viewId, name, parentId, index));
    }

}