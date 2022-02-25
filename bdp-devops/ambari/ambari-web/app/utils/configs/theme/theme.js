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

var App = require('app');

App.configTheme = Em.Object.create({

  /**
   * Resolve config theme conditions
   * in order to correctly calculate config errors number of service
   * @param {Array} configs
   */
  resolveConfigThemeConditions: function (configs) {
    App.ThemeCondition.find().forEach(function (configCondition) {
      var _configs = Em.A(configCondition.get('configs'));
      if (configCondition.get("resource") === 'config' && _configs.length > 0) {
        var isConditionTrue = this.calculateConfigCondition(configCondition.get("if"), configs);
        var action = isConditionTrue ? configCondition.get("then") : configCondition.get("else");
        if (configCondition.get('id')) {
          var valueAttributes = action.property_value_attributes;
          if (valueAttributes && !Em.none(valueAttributes.visible)) {
            var themeResource = this.getThemeResource(configCondition);
            if (themeResource) {
              themeResource.get('configProperties').forEach(function (_configId) {
                configs.forEach(function (item) {
                  if (App.config.configId(item.get('name'), item.get('filename')) === _configId) {
                    // if config has already been hidden by condition with "subsection" or "subsectionTab" type
                    // then ignore condition of "config" type
                    if (configCondition.get('type') === 'config' && item.get('hiddenBySection')) return false;
                    item.set('hiddenBySection', !valueAttributes.visible);
                  }
                }, this);
              }, this);
            }
          }
        }
      }
    }, this);
  },

  /**
   *
   * @param {App.ThemeCondition} configCondition
   * @returns {?Em.Object}
   */
  getThemeResource: function (configCondition) {
    var themeResource = null;

    if (configCondition.get('type') === 'subsection') {
      themeResource = App.SubSection.find().findProperty('name', configCondition.get('name'));
    } else if (configCondition.get('type') === 'subsectionTab') {
      themeResource = App.SubSectionTab.find().findProperty('name', configCondition.get('name'));
    } else if (configCondition.get('type') === 'config') {
      //simulate section wrapper for condition type "config"
      themeResource = Em.Object.create({
        configProperties: [App.config.configId(configCondition.get('configName'), configCondition.get('fileName'))]
      });
    }
    return themeResource;
  },

  /**
   *
   * @param {Array} configs
   * @param {?Array} storedConfigs
   * @returns {{add: Array, delete: Array}}
   */
  getConfigThemeActions: function (configs, storedConfigs) {
    //config actions for changed configs should be only effective
    var configActions = this.getConfigActions(configs, storedConfigs);

    var componentsToAdd = [];
    var componentsToDelete = [];
    configActions.forEach(function (_action) {
      var isConditionTrue = this.calculateConfigCondition(_action.get('if'), configs);
      var action = isConditionTrue ? _action.get("then") : _action.get("else");
      switch (action) {
        case 'add':
          componentsToAdd.push(_action.get('hostComponent'));
          break;
        case 'delete':
          componentsToDelete.push(_action.get('hostComponent'));
          break;
      }
    }, this);

    return {
      add: componentsToAdd,
      delete: componentsToDelete
    };
  },

  /**
   *
   * @param {Array} configs
   * @param {Array} storedConfigs
   * @returns {Array}
   */
  getConfigActions: function (configs, storedConfigs) {
    return App.ConfigAction.find().filter(function (item) {
      var isAnyConfigAbsent = false;
      var configChanged = false;
      item.get("configs").forEach(function (_config) {
        var config = configs.filterProperty('filename', _config.fileName).findProperty('name', _config.configName);
        if (!config) {
          isAnyConfigAbsent = true;
        } else {
          configChanged = configChanged || config.get('value') !== (config.get('savedValue') || config.get('recommendedValue'));
          var storedConfig = storedConfigs.filterProperty('filename', _config.fileName).findProperty('name', _config.configName);
          if (storedConfig) {
            configChanged = configChanged || config.get('value') !== storedConfig.value;
          }
        }
      }, this);
      return !isAnyConfigAbsent && configChanged;
    }, this);
  },

  /**
   * ifStatement format: ${site-name/config-name}
   * @param {string} ifStatement
   * @param {Array} serviceConfigs
   * @returns {boolean}
   */
  calculateConfigCondition: function (ifStatement, serviceConfigs) {
    // Split `if` statement if it has logical operators
    var ifStatementRegex = /(&&|\|\|)/;
    var IfConditions = ifStatement.split(ifStatementRegex);
    var allConditionResult = [];
    IfConditions.forEach(function (_condition) {
      var condition = _condition.trim();
      if (condition === '&&' || condition === '||') {
        allConditionResult.push(_condition);
      } else {
        var splitIfCondition = condition.split('===');
        var ifCondition = splitIfCondition[0];
        var result = splitIfCondition[1] || "true";
        var parseIfConditionVal = ifCondition;
        var regex = /\$\{.*?\}/g;
        var configStrings = ifCondition.match(regex);
        configStrings.forEach(function (_configString) {
          var configObject = _configString.substring(2, _configString.length - 1).split("/");
          var config = serviceConfigs.filterProperty('filename', configObject[0] + '.xml').findProperty('name', configObject[1]);
          if (config) {
            var configValue = Em.get(config, 'value');
            parseIfConditionVal = parseIfConditionVal.replace(_configString, configValue);
          }
        }, this);
        var conditionResult = window.eval(JSON.stringify(parseIfConditionVal.trim())) === result.trim();
        allConditionResult.push(conditionResult);
      }
    }, this);
    return Boolean(window.eval(allConditionResult.join('')));
  }

});
