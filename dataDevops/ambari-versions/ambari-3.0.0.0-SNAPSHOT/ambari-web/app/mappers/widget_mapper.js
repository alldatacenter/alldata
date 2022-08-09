/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


App.widgetMapper = App.QuickDataMapper.create({
  model: App.Widget,
  config: {
    id: 'id',
    layout_id: 'layout_id',
    widget_name: 'widget_name',
    default_order: 'default_order',
    widget_type: 'widget_type',
    service_name: 'service_name',
    section_name: 'section_name',
    time_created: 'time_created',
    author: 'author',
    properties: 'properties',
    metrics: 'metrics',
    values: 'values',
    description: 'description',
    scope: 'scope',
    tag: 'tag'
  },
  map: function (json) {
    if (!this.get('model')) return;

    if (json.widgets) {
      var result = [];

      json.widgets.forEach(function (item, index) {
        item.WidgetInfo.section_name = json.section_name;
        item.WidgetInfo.layout_id = json.id;
        item.WidgetInfo.metrics = JSON.parse(item.WidgetInfo.metrics);
        item.WidgetInfo.properties = JSON.parse(item.WidgetInfo.properties);
        item.WidgetInfo.values = JSON.parse(item.WidgetInfo.values);
        item.WidgetInfo.default_order = (index + 1);
        result.push(this.parseIt(item.WidgetInfo, this.config));
      }, this);

      App.store.safeLoadMany(this.get('model'), result);
    }
  }
});
