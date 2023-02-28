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
package io.datavines.server.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.bo.catalog.tag.TagCreate;
import io.datavines.server.api.dto.vo.catalog.CatalogTagVO;
import io.datavines.server.repository.entity.catalog.CatalogEntityTagRel;
import io.datavines.server.repository.entity.catalog.CatalogTag;
import io.datavines.server.repository.mapper.CatalogTagMapper;
import io.datavines.server.repository.service.CatalogEntityTagRelService;
import io.datavines.server.repository.service.CatalogTagService;
import io.datavines.server.utils.ContextHolder;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.datavines.core.enums.Status.CATALOG_ENTITY_TAG_EXIST_ERROR;

@Service("catalogTagService")
public class CatalogTagServiceImpl extends ServiceImpl<CatalogTagMapper, CatalogTag> implements CatalogTagService {

    @Autowired
    private CatalogEntityTagRelService catalogEntityTagRelService;

    @Override
    public long create(TagCreate tagCreate) {
        if (isExist(tagCreate.getName())) {
            throw new DataVinesServerException(Status.CATALOG_TAG_EXIST_ERROR, tagCreate.getName());
        }

        CatalogTag catalogTag = new CatalogTag();
        BeanUtils.copyProperties(tagCreate, catalogTag);

        catalogTag.setUuid(UUID.randomUUID().toString());
        catalogTag.setCreateTime(LocalDateTime.now());
        catalogTag.setUpdateTime(LocalDateTime.now());
        catalogTag.setCreateBy(ContextHolder.getUserId());
        catalogTag.setUpdateBy(ContextHolder.getUserId());
        baseMapper.insert(catalogTag);
        return catalogTag.getId();
    }

    @Override
    public boolean delete(String uuid) {
        return remove(new QueryWrapper<CatalogTag>().eq("uuid", uuid));
    }

    @Override
    public List<CatalogTag> listByCategoryUUID(String categoryUUID) {
        return list(new QueryWrapper<CatalogTag>().eq("category_uuid", categoryUUID));
    }

    @Override
    public List<CatalogTag> listByEntityUUID(String entityUUID) {
        List<CatalogEntityTagRel> relList = catalogEntityTagRelService
                .list(new QueryWrapper<CatalogEntityTagRel>().eq("entity_uuid", entityUUID));

        if (CollectionUtils.isEmpty(relList)) {
            return null;
        }

        return list(new QueryWrapper<CatalogTag>().in("uuid", relList.stream().map(CatalogEntityTagRel::getTagUuid).collect(Collectors.toList())));
    }

    @Override
    public boolean addEntityTagRel(String entityUUID, String tagUUID) {
        CatalogEntityTagRel rel = new CatalogEntityTagRel();
        List<CatalogEntityTagRel> list = catalogEntityTagRelService.list(new QueryWrapper<CatalogEntityTagRel>()
                .eq("entity_uuid", entityUUID)
                .eq("tag_uuid", tagUUID));
        if (CollectionUtils.isNotEmpty(list)) {
            throw new DataVinesServerException(CATALOG_ENTITY_TAG_EXIST_ERROR);
        }
        rel.setEntityUuid(entityUUID);
        rel.setTagUuid(tagUUID);
        rel.setCreateBy(ContextHolder.getUserId());
        rel.setUpdateBy(ContextHolder.getUserId());
        rel.setCreateTime(LocalDateTime.now());
        rel.setUpdateTime(LocalDateTime.now());
        return catalogEntityTagRelService.save(rel);
    }

    @Override
    public boolean deleteEntityTagRel(String entityUUID, String tagUUID) {
        return catalogEntityTagRelService.remove(new QueryWrapper<CatalogEntityTagRel>()
                .eq("entity_uuid", entityUUID)
                .eq("tag_uuid", tagUUID));
    }

    @Override
    public List<CatalogTagVO> listByWorkSpaceId(Long workSpaceId) {
        return baseMapper.listByWorkSpaceId(workSpaceId);
    }

    private boolean isExist(String name) {
        CatalogTag tag = baseMapper.selectOne(new QueryWrapper<CatalogTag>().eq("name", name));
        return tag != null;
    }

}
