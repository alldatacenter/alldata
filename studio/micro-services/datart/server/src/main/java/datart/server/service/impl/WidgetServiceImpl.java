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

package datart.server.service.impl;

import datart.core.common.UUIDGenerator;
import datart.core.entity.RelWidgetElement;
import datart.core.entity.RelWidgetWidget;
import datart.core.entity.Widget;
import datart.core.mappers.ext.RelWidgetElementMapperExt;
import datart.core.mappers.ext.RelWidgetWidgetMapperExt;
import datart.core.mappers.ext.WidgetMapperExt;
import datart.security.base.ResourceType;
import datart.server.base.params.WidgetCreateParam;
import datart.server.base.params.WidgetRelParam;
import datart.server.base.params.WidgetUpdateParam;
import datart.server.service.BaseService;
import datart.server.service.FileService;
import datart.server.service.WidgetService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WidgetServiceImpl extends BaseService implements WidgetService {

    private final WidgetMapperExt widgetMapper;

    private final RelWidgetElementMapperExt rweMapper;

    private final RelWidgetWidgetMapperExt rwwMapper;

    private final FileService fileService;

    public WidgetServiceImpl(WidgetMapperExt widgetMapper,
                             RelWidgetElementMapperExt rweMapper,
                             RelWidgetWidgetMapperExt rwwMapper,
                             FileService fileService) {
        this.widgetMapper = widgetMapper;
        this.rweMapper = rweMapper;
        this.rwwMapper = rwwMapper;
        this.fileService = fileService;
    }

    @Override
    public void requirePermission(Widget entity, int permission) {

    }

    @Override
    @Transactional
    public List<Widget> createWidgets(List<WidgetCreateParam> createParams) {
        if (CollectionUtils.isEmpty(createParams)) {
            return null;
        }
        ArrayList<Widget> widgets = new ArrayList<>();
        ArrayList<RelWidgetElement> elements = new ArrayList<>();
        for (WidgetCreateParam createParam : createParams) {
            Widget widget = new Widget();
            BeanUtils.copyProperties(createParam, widget);
            if (StringUtils.isBlank(widget.getId())) {
                widget.setId(UUIDGenerator.generate());
            }
            widget.setCreateBy(getCurrentUser().getId());
            widget.setCreateTime(new Date());
            widgets.add(widget);
            if (!StringUtils.isBlank(createParam.getDatachartId())) {
                elements.add(createRelWidgetElement(widget.getId(), ResourceType.DATACHART.name(), createParam.getDatachartId()));
            }
            if (!CollectionUtils.isEmpty(createParam.getViewIds())) {
                for (String viewId : createParam.getViewIds()) {
                    elements.add(createRelWidgetElement(widget.getId(), ResourceType.VIEW.name(), viewId));
                }
            }
            // insert widget relations
            updateWidgetRelations(createParam.getId(), createParam.getRelations());
        }
        //insert widgets
        if (!CollectionUtils.isEmpty(widgets)) {
            widgetMapper.batchInsert(widgets);
        }
        // insert widget elements
        if (!CollectionUtils.isEmpty(elements)) {
            rweMapper.batchInsert(elements);
        }

        return widgets;
    }

    @Override
    @Transactional
    public boolean updateWidgets(List<WidgetUpdateParam> updateParams) {
        if (CollectionUtils.isEmpty(updateParams)) {
            return false;
        }
        LinkedList<RelWidgetElement> elements = new LinkedList<>();
        LinkedList<String> elementToDelete = new LinkedList<>();
        List<Widget> widgetsToUpdate = updateParams.stream().map(updateParam -> {
            if (!StringUtils.isBlank(updateParam.getDatachartId())) {
                elements.add(createRelWidgetElement(updateParam.getId(), ResourceType.DATACHART.name(), updateParam.getDatachartId()));
            }
            if (!CollectionUtils.isEmpty(updateParam.getViewIds())) {
                for (String viewId : updateParam.getViewIds()) {
                    elements.add(createRelWidgetElement(updateParam.getId(), ResourceType.VIEW.name(), viewId));
                }
            }

            // update widget relations
            updateWidgetRelations(updateParam.getId(), updateParam.getRelations());

            elementToDelete.add(updateParam.getId());
            Widget widget = new Widget();
            BeanUtils.copyProperties(updateParam, widget);
            widget.setUpdateTime(new Date());
            widget.setUpdateBy(getCurrentUser().getId());
            return widget;
        }).collect(Collectors.toList());
        // update widgets
        if (!CollectionUtils.isEmpty(widgetsToUpdate)) {
            widgetMapper.batchUpdate(widgetsToUpdate);
        }
        // update widget elements
        if (!CollectionUtils.isEmpty(elementToDelete)) {
            rweMapper.deleteByWidgets(elementToDelete);
        }
        if (!CollectionUtils.isEmpty(elements)) {
            rweMapper.batchInsert(elements);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean deleteWidgets(List<String> widgetIds) {

        if (CollectionUtils.isEmpty(widgetIds)) {
            return false;
        }
//        for (String id : widgetIds) {
//            fileService.deleteFiles(FileOwner.WIDGET, id);
//        }
        return widgetMapper.deleteWidgets(widgetIds) > 0;
    }


    private void updateWidgetRelations(String sourceId, List<WidgetRelParam> relations) {
        // delete targets
        rwwMapper.deleteBySourceId(sourceId);

        if (CollectionUtils.isEmpty(relations)) {
            return;
        }
        List<RelWidgetWidget> relWidgetWidgets = relations.stream().map(param -> {
            RelWidgetWidget rel = new RelWidgetWidget();
            BeanUtils.copyProperties(param, rel);
            rel.setId(UUIDGenerator.generate());
            return rel;
        }).collect(Collectors.toList());
        rwwMapper.batchInsert(relWidgetWidgets);
    }


    private RelWidgetElement createRelWidgetElement(String widgetId, String relType, String relId) {
        RelWidgetElement element = new RelWidgetElement();
        element.setId(UUIDGenerator.generate());
        element.setCreateBy(getCurrentUser().getId());
        element.setCreateTime(new Date());
        element.setWidgetId(widgetId);
        element.setRelId(relId);
        element.setRelType(relType);
        return element;
    }

}
