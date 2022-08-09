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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require("app");

App.themesMapper = App.QuickDataMapper.create({
  tabModel: App.Tab,
  sectionModel: App.Section,
  subSectionModel: App.SubSection,
  subSectionTabModel: App.SubSectionTab,
  themeConditionModel: App.ThemeCondition,

  tabConfig: {
    "id": "name",
    "name": "name",
    "display_name": "display-name",
    "columns": "layout.tab-columns",
    "rows": "layout.tab-rows",
    "service_name": "service_name",
    "sections_key": "sections"
  },

  sectionConfig: {
    "name": "name",
    "display_name": "display-name",
    "row_index": "row-index",
    "column_index": "column-index",
    "row_span": "row-span",
    "column_span": "column-span",
    "section_columns": "section-columns",
    "section_rows": "section-rows",
    "tab_id": "tab_id"
  },

  subSectionConfig: {
    "name": "name",
    "display_name": "display-name",
    "border": "border",
    "row_index": "row-index",
    "column_index": "column-index",
    "column_span": "column-span",
    "row_span": "row-span",
    "configProperties": "config_properties",
    "section_id": "section_id",
    "depends_on": "depends-on",
    "left_vertical_splitter": "left-vertical-splitter"
  },

  subSectionTabConfig: {
    "id": "name",
    "name": "name",
    "display_name": "display-name",
    "depends_on": "depends-on",
    "sub_section_id": "sub_section_id"
  },

  resetModels: function() {
    this.get('tabModel').find().clear();
    this.get('sectionModel').find().clear();
    this.get('subSectionModel').find().clear();
    this.get('subSectionTabModel').find().clear();
    this.get('themeConditionModel').find().clear();
  },

  /**
   * Mapper function for tabs
   *
   * @param json
   * @param [serviceNames]
   */
  map: function (json, serviceNames) {
    console.time('App.themesMapper execution time');
    var tabs = [];
    json.items.forEach(function(item) {
      this.mapThemeLayouts(item, tabs);
      this.mapThemeConfigs(item);
      this.mapThemeWidgets(item);
    }, this);

    App.store.safeLoadMany(this.get("tabModel"), tabs);
    this.generateAdvancedTabs(serviceNames);
    console.timeEnd('App.themesMapper execution time');
  },

  /**
   * Bootstrap tab objects and link sections with subsections.
   *
   * @param {Object} json - json to parse
   * @param {Object[]} tabs - tabs list
   */
  mapThemeLayouts: function (json, tabs) {
    var serviceName = Em.get(json, "ThemeInfo.service_name");
    var themeName = Em.get(json, "ThemeInfo.theme_data.Theme.name");
    Em.getWithDefault(json, "ThemeInfo.theme_data.Theme.configuration.layouts", []).forEach(function (layout) {
      if (layout.tabs) {
        layout.tabs.forEach(function (tab) {
          var parsedTab = this.parseIt(tab, this.get("tabConfig"));
          parsedTab.id = serviceName + "_" + tab.name;
          parsedTab.service_name = serviceName;
          parsedTab.theme_name = themeName;

          if (Em.get(tab, "layout.sections")) {
            var sections = [];
            Em.get(tab, "layout.sections").forEach(function (section) {
              var parsedSection = this.parseIt(section, this.get("sectionConfig"));
              parsedSection.id = section.name + '_' + parsedTab.id;
              parsedSection.tab_id = parsedTab.id;
              this.loadSubSections(section, parsedSection, serviceName, themeName);
              sections.push(parsedSection);
            }, this);

            App.store.safeLoadMany(this.get("sectionModel"), sections);
            parsedTab.sections = sections.mapProperty("id");
          }

          tabs.push(parsedTab);
        }, this);
      }

    }, this);
  },

  loadSubSections: function(section, parsedSection, serviceName, themeName) {
    if (section.subsections) {
      var subSections = [];
      var subSectionConditions = [];
      section.subsections.forEach(function(subSection) {
        var parsedSubSection = this.parseIt(subSection, this.get("subSectionConfig"));
        parsedSubSection.section_id = parsedSection.id;
        parsedSubSection.id = parsedSubSection.name + '_' + serviceName + '_' + themeName;
        parsedSubSection.theme_name = themeName;

        this.loadSubSectionTabs(subSection, parsedSubSection, themeName);
        if (parsedSubSection['depends_on']) {
          subSectionConditions.push(parsedSubSection);
        }
        subSections.push(parsedSubSection);
      }, this);
      if (subSectionConditions.length) {
        var type = 'subsection';
        this.mapThemeConditions(subSectionConditions, type, themeName);
      }
      App.store.safeLoadMany(this.get("subSectionModel"), subSections);
      parsedSection.sub_sections = subSections.mapProperty("id");
    }
  },


  loadSubSectionTabs: function(subSection, parsedSubSection, themeName) {
    if (subSection['subsection-tabs']) {
      var subSectionTabs = [];
      var subSectionTabConditions = [];

      subSection['subsection-tabs'].forEach(function (subSectionTab) {
        var parsedSubSectionTab = this.parseIt(subSectionTab, this.get("subSectionTabConfig"));
        parsedSubSectionTab.sub_section_id = parsedSubSection.id;
        parsedSubSectionTab.theme_name = themeName;
        if (parsedSubSectionTab['depends_on']) {
          subSectionTabConditions.push(parsedSubSectionTab);
        }
        subSectionTabs.push(parsedSubSectionTab);
      }, this);
      subSectionTabs[0].is_active = true;
      if (subSectionTabConditions.length) {
        var type = 'subsectionTab';
        this.mapThemeConditions(subSectionTabConditions, type, themeName);
      }
      App.store.safeLoadMany(this.get("subSectionTabModel"), subSectionTabs);
      parsedSubSection.sub_section_tabs = subSectionTabs.mapProperty("id");
    }
  },

  /**
   * create tie between <code>stackConfigProperty<code> and <code>subSection<code>
   *
   * @param {Object} json - json to parse
   */
  mapThemeConfigs: function(json) {
    var serviceName = Em.get(json, "ThemeInfo.service_name");
    var themeName = Em.get(json, "ThemeInfo.theme_data.Theme.name");
    Em.getWithDefault(json, "ThemeInfo.theme_data.Theme.configuration.placement.configs", []).forEach(function(configLink) {
      var configId = this.getConfigId(configLink);
      var subSectionId = configLink["subsection-name"] ? configLink["subsection-name"] + '_' + serviceName + '_' + themeName : false;
      var subSectionTabId = configLink["subsection-tab-name"];
      var configProperty = App.configsCollection.getConfig(configId);
      var dependsOnConfigs = configLink["depends-on"] || [];

      if (subSectionTabId) {
        var subSectionTab = App.SubSectionTab.find(subSectionTabId);
      } else if (subSectionId) {
        var subSection = App.SubSection.find(subSectionId);
      }

      if (configProperty && subSection && subSection.get('isLoaded')) {
        if (!subSection.get('configProperties').contains(configProperty.id)) {
          subSection.set('configProperties', subSection.get('configProperties').concat(configProperty.id));
        }
      } else if (configProperty && subSectionTab && subSectionTab.get('isLoaded')) {
        if (!subSectionTab.get('configProperties').contains(configProperty.id)) {
          subSectionTab.set('configProperties', subSectionTab.get('configProperties').concat(configProperty.id));
        }
      } else {
        configProperty = this.getConfigByAttributes(configProperty, configLink, subSection, subSectionTab, serviceName);
      }

      // map all the configs which conditionally affect the value attributes of a config
      if (dependsOnConfigs && dependsOnConfigs.length && configProperty) {
        this.mapThemeConfigConditions(dependsOnConfigs, configProperty);
      }

    }, this);
  },

  getConfigByAttributes: function(configProperty, configLink, subSection, subSectionTab, serviceName) {
    var valueAttributes = configLink["property_value_attributes"];
    if (valueAttributes) {
      var isUiOnlyProperty = valueAttributes["ui_only_property"];
      var isCopy = valueAttributes["copy"] || '';
      // UI only configs are mentioned in the themes for supporting widgets that is not intended for setting a value
      // And thus is affiliated with fake config property termed as ui only config property
      if (isUiOnlyProperty && subSection) {
        var split = configLink.config.split("/");
        var fileName =  split[0] + '.xml', configName = split[1], id = App.config.configId(configName, fileName);
        var uiOnlyConfig = App.configsCollection.getConfig(id);
        if (!uiOnlyConfig) {
          configProperty = {
            id: id,
            isRequiredByAgent: false,
            showLabel: false,
            isOverridable: false,
            recommendedValue: true,
            name: JSON.parse('"' + configName + '"'),
            isUserProperty: false,
            filename: fileName,
            fileName: fileName,
            isNotSaved: false,
            serviceName: serviceName,
            copy: isCopy,
            stackName: App.get('currentStackName'),
            stackVersion: App.get('currentStackVersionNumber')
          };
          App.configsCollection.add(configProperty);

          if (!subSection.get('configProperties').contains(configProperty.id)) {
            subSection.set('configProperties', subSection.get('configProperties').concat(configProperty.id));
          }
        }
      }
    }
    return configProperty;
  },

  /**
   *
   * @param configConditions: Array
   * @param configProperty: DS.Model Object (App.StackConfigProperty)
   */
  mapThemeConfigConditions: function(configConditions, configProperty) {
    var configConditionsCopy = [];
    configConditions.forEach(function(_configCondition, index){
      var configCondition = $.extend({}, _configCondition);
      configCondition.id = configProperty.id + '_' + index;
      configCondition.config_name =  configProperty.name;
      configCondition.file_name =  configProperty.filename;
      if (_configCondition.configs && _configCondition.configs.length) {
        configCondition.configs = _configCondition.configs.map(function (item) {
          var result = {};
          result.fileName = item.split('/')[0] + '.xml';
          result.configName = item.split('/')[1];
          return result;
        });
      }

      configCondition.resource = _configCondition.resource || 'config';
      configCondition.type = _configCondition.type || 'config';

      configConditionsCopy.pushObject(configCondition);
    }, this);

    App.store.safeLoadMany(this.get("themeConditionModel"), configConditionsCopy);
  },

  /**
   *
   * @param subSections: Array
   * @param type: {String} possible values: `subsection` or `subsectionTab`
   */
  mapThemeConditions: function(subSections, type, themeName) {
    var subSectionConditionsCopy = [];
    subSections.forEach(function(_subSection){
      var subSectionConditions = _subSection['depends_on'];
      subSectionConditions.forEach(function(_subSectionCondition, index){
        var subSectionCondition = $.extend({},_subSectionCondition);
        subSectionCondition.id = _subSection.id + '_' + index;
        subSectionCondition.name = _subSection.name;
        subSectionCondition.theme_name = themeName;
        if (_subSectionCondition.configs && _subSectionCondition.configs.length) {
          subSectionCondition.configs = _subSectionCondition.configs.map(function (item) {
            var result = {};
            result.fileName = item.split('/')[0] + '.xml';
            result.configName = item.split('/')[1];
            return result;
          });
        }

        subSectionCondition.resource = _subSectionCondition.resource || 'config';
        subSectionCondition.type = _subSectionCondition.type || type;
        subSectionConditionsCopy.pushObject(subSectionCondition);
      }, this);
    }, this);
    App.store.safeLoadMany(this.get("themeConditionModel"), subSectionConditionsCopy);
  },

  /**
   * add widget object to <code>stackConfigProperty<code>
   *
   * @param {Object} json - json to parse
   */
  mapThemeWidgets: function(json) {
    var themeName = Em.get(json, 'ThemeInfo.theme_data.Theme.name');
    Em.getWithDefault(json, "ThemeInfo.theme_data.Theme.configuration.widgets", []).forEach(function(widget) {
      var configId = this.getConfigId(widget);
      var configProperty = App.configsCollection.getConfig(configId);

      if (configProperty) {
        Em.set(configProperty, 'widget', widget.widget);
        Em.set(configProperty, 'widgetType', Em.get(widget, 'widget.type'));
        if (themeName === 'default') {
          Em.set(configProperty, 'isInDefaultTheme', true);
        }
      } else {
        var split = widget.config.split("/");
        var fileName =  split[0] + '.xml';
        var configName = split[1];
        var uiOnlyProperty = App.configsCollection.getConfigByName(configName, fileName);
        if (uiOnlyProperty) {
          uiOnlyProperty.set('widget', widget.widget);
          uiOnlyProperty.set('widgetType', Em.get(widget, 'widget.type'));
          if (themeName === 'default') {
            Em.set(uiOnlyProperty, 'isInDefaultTheme', true);
          }
        }
      }
    }, this);
  },

  /**
   * transform info from json to config id
   * @param {Object} json
   * @returns {string|null}
   */
  getConfigId: function(json) {
    if (json && json.config && typeof json.config === "string") {
      // symbols until first slash is file-name and the rest is config-name
      const fileName = json.config.substr(0, json.config.indexOf('/'));
      const configName = json.config.substr(json.config.indexOf('/') + 1);
      return App.config.configId(configName, fileName);
    } else {
      return null;
    }
  },

  /**
   * generates Advanced tabs for selected or all services
   * @param {[string]} [serviceNames=null]
   */
  generateAdvancedTabs: function(serviceNames) {
    serviceNames = Em.isArray(serviceNames) ? serviceNames : App.StackService.find().mapProperty('serviceName');
    var advancedTabs = [];
    serviceNames.forEach(function(serviceName) {
      advancedTabs.pushObject({
        id: serviceName + '_advanced',
        name: 'advanced',
        display_name: 'Advanced',
        is_advanced: true,
        service_name: serviceName
      });
    });
    App.store.safeLoadMany(this.get("tabModel"), advancedTabs);
  }
});
