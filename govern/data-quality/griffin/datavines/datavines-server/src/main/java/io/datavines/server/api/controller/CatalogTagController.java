/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.api.controller;

import io.datavines.core.aop.RefreshToken;
import io.datavines.core.constant.DataVinesConstants;
import io.datavines.server.api.dto.bo.catalog.tag.TagCategoryCreate;
import io.datavines.server.api.dto.bo.catalog.tag.TagCreate;
import io.datavines.server.api.dto.vo.catalog.CatalogTagVO;
import io.datavines.server.repository.entity.catalog.CatalogTag;
import io.datavines.server.repository.entity.catalog.CatalogTagCategory;
import io.datavines.server.repository.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(value = "catalog", tags = "catalog", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/catalog/tag", produces = MediaType.APPLICATION_JSON_VALUE)
@RefreshToken
public class CatalogTagController {

    @Autowired
    private CatalogTagCategoryService tagCategoryService;

    @Autowired
    private CatalogTagService tagService;

    @ApiOperation(value = "create tag category", response = Long.class)
    @PostMapping(value = "/category", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createTagCategory(@Valid @RequestBody TagCategoryCreate categoryCreate) {
        return tagCategoryService.create(categoryCreate);
    }

    @ApiOperation(value = "get tag category list", response = CatalogTagCategory.class, responseContainer = "list")
    @GetMapping(value = "/category/list/{workspaceId}")
    public Object listCategoryByWorkSpaceId(@PathVariable Long workspaceId) {
        return tagCategoryService.listByWorkSpaceId(workspaceId);
    }

    @ApiOperation(value = "delete tag category", response = boolean.class)
    @DeleteMapping(value = "/category/{uuid}")
    public Object deleteCategory(@PathVariable String uuid) {
        return tagCategoryService.delete(uuid);
    }

    @ApiOperation(value = "create tag", response = Long.class)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createTag(@Valid @RequestBody TagCreate tagCreate) {
        return tagService.create(tagCreate);
    }

    @ApiOperation(value = "get tag list by workspace id", response = CatalogTagVO.class, responseContainer = "list")
    @GetMapping(value = "/list-in-workspace/{workspaceId}")
    public Object listTagByWorkSpaceId(@PathVariable Long workspaceId) {
        return tagService.listByWorkSpaceId(workspaceId);
    }

    @ApiOperation(value = "get tag list by category uuid", response = CatalogTag.class, responseContainer = "list")
    @GetMapping(value = "/list-in-category/{categoryUUID}")
    public Object listTagByCategoryUUID(@PathVariable String categoryUUID) {
        return tagService.listByCategoryUUID(categoryUUID);
    }

    @ApiOperation(value = "get tag list by entity uuid", response = CatalogTag.class, responseContainer = "list")
    @GetMapping(value = "/list-in-entity/{entityUUID}")
    public Object listTagByEntityUUID(@PathVariable String entityUUID) {
        return tagService.listByEntityUUID(entityUUID);
    }

    @ApiOperation(value = "delete category", response = boolean.class)
    @DeleteMapping(value = "/{uuid}")
    public Object deleteTag(@PathVariable String uuid) {
        return tagService.delete(uuid);
    }

    @ApiOperation(value = "entity add tag", response = boolean.class)
    @PostMapping(value = "/entity-tag")
    public Object addEntityTagRel(@RequestParam("entityUUID") String entityUUID,
                                  @RequestParam("tagUUID") String tagUUID) {
        return tagService.addEntityTagRel(entityUUID, tagUUID);
    }

    @ApiOperation(value = "delete entity tag", response = boolean.class)
    @DeleteMapping(value = "/entity-tag")
    public Object deleteEntityTagRel(@RequestParam("entityUUID") String entityUUID,
                                     @RequestParam("tagUUID") String tagUUID) {
        return tagService.deleteEntityTagRel(entityUUID, tagUUID);
    }
}
