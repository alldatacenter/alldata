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
var dbInfo = require('data/db_properties_info') || {};
var dbUtils = require('utils/configs/database');

/**
 * Combo box widget view for config property.
 * @type {Em.View}
 */
App.ComboConfigWidgetView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/combo_config_widget'),
  classNames: ['widget-config', 'combo-widget'],
  supportSwitchToTextBox: true,
  /**
   * Object with following structure:
   * {String} .value - value in widget format
   * {Object[]} .valuesList - map of entries and entry_labels
   *   {String} .configValue - value in config format
   *   {String} .widgetValue - value in widget format
   *
   * @property content
   * @type {Em.Object}
   */
  content: null,

  didInsertElement: function() {
    this.initWidget();
    this._super();
    this.toggleWidgetState();
    this.initPopover();
    this.disableSwitchToTextBox();
    this.addObserver('config.stackConfigProperty.valueAttributes.entries.[]', this, this.updateValuesList);
    this.addObserver('controller.forceUpdateBoundaries', this, this.updateValuesList);
    this.addObserver('config.value', this, this.isValueCompatibleWithWidget);
    this.addCustomMessage();
  },

  customMessageServiceMapping: [
    {
      serviceName: 'HIVE', dbConfigName: 'hive_database'
    },
    {
      serviceName: 'RANGER', dbConfigName: 'DB_FLAVOR'
    },
    {
      serviceName: 'RANGER_KMS', dbConfigName: 'DB_FLAVOR'
    }
  ],

  addCustomMessage: function () {
    // show warning alert about downloading db connector for services with databases
    if (this.isCustomMessageRequired()) {
      this.set('config.additionalView', Em.View.extend({
        template: Em.Handlebars.compile('<div class="alert alert-warning enhanced-configs">{{{view.message}}}</div>'),
        message: function () {
          var selectedDb = dbUtils.getDBType(this.get('config.value'));
          var dbData = dbInfo.dpPropertiesMap[selectedDb];
          var driver_jar = dbData.sql_jar_connector ? dbData.sql_jar_connector.split("/").pop() : 'driver.jar';
          return Em.I18n.t('services.service.config.database.msg.jdbcSetup.detailed').format(
            dbData.db_name,
            dbData.db_type,
            driver_jar,
            dbData.driver_download_url,
            dbData.driver_download_url,
            dbData.driver_name,
            this.get('config.serviceName').toCapital()
          );
        }.property('config.value'),
        config: this.get('config')
      }));
    }
  },

  isCustomMessageRequired: function () {
    var self = this;
    return this.get('customMessageServiceMapping').find(function (configMap) {
      return configMap['serviceName'].toLowerCase() === self.get('config.serviceName').toLowerCase() && configMap['dbConfigName'].toLowerCase() === self.get('config.name').toLowerCase()
    });
  },

  disableSwitchToTextBox: function () {
    var valueAttributes = this.get('config.valueAttributes');
    if (valueAttributes && valueAttributes.hasOwnProperty('entriesEditable') && !valueAttributes.entriesEditable) {
      this.set('supportSwitchToTextBox', false);
    }
  },

  /**
   * Update options list by recommendations
   * @method updateValuesList
   */
  updateValuesList: function() {
    if (!this.get('content')) {
      this.set('content', Em.Object.create({}));
    }
    this.set('content.valuesList', this.convertToWidgetUnits(this.get('config.stackConfigProperty.valueAttributes')));
  },

  /**
   * Generate content for view. Set values map and current value.
   *
   * @method generateContent
   */
  initWidget: function() {
    this.set('content', Em.Object.create({}));
    this.set('content.valuesList', this.convertToWidgetUnits(this.get('config.stackConfigProperty.valueAttributes')));
    this.set('content.value', this.generateWidgetValue(this.get('config.value')));
  },

  /**
   * Generate values map according to widget/value format.
   *
   * @method convertToWidgetUnits
   * @param {Object} valueAttributes
   * @returns {Object[]} - values list map @see content.valuesList
   */
  convertToWidgetUnits: function(valueAttributes) {
    return Em.get(valueAttributes, 'entries').map(function(item) {
      return Em.Object.create({
        configValue: item.value,
        widgetValue: item.label || item.value
      });
    });
  },

  /**
   * Get widget value by specified config value.
   *
   * @method generateWidgetValue
   * @param {String} value - value in config property format
   * @returns {String}
   */
  generateWidgetValue: function(value) {
    if (this.isValueCompatibleWithWidget()) {
      return this.get('content.valuesList').findProperty('configValue', value).get('widgetValue');
    }
    return value;
  },

  /**
   * Get config value by specified widget value.
   *
   * @method generateConfigValue
   * @param {String} value - value in widget property format
   * @returns {String}
   */
  generateConfigValue: function(value) {
    return this.get('content.valuesList').findProperty('widgetValue', value).get('configValue');
  },

  /**
   * Action to set config value.
   *
   * @method setConfigValue
   * @param {Object} e
   */
  setConfigValue: function(e) {
    this.set('config.value', e.context);
    this.set('content.value', this.generateWidgetValue(e.context));
    if (this.get('config.previousValue') != this.get('config.value')) {
      this.sendRequestRorDependentConfigs(this.get('config'));
    }
    this.set('config.previousValue', this.get('config.value'));
  },

  /**
   * @override App.ConfigWidgetView.restoreValue
   * @method restoreValue
   */
  restoreValue: function() {
    this.setConfigValue({ context: this.get('config.savedValue') });
    if (this.get('config.supportsFinal')) {
      this.get('config').set('isFinal', this.get('config.savedIsFinal'));
    }
  },

  /**
   * @method setRecommendedValue
   */
  setRecommendedValue: function () {
    this.setConfigValue({ context: this.get('config.recommendedValue')});
    if (this.get('config.supportsFinal')) {
      this.get('config').set('isFinal', this.get('config.recommendedIsFinal'));
    }
  },

  /**
   * Delegate event from text input in combo widget to trigger dropdown
   */
  click: function(event) {
    if (!this.get('disabled') && event.target.className.contains('ember-text-field')) {
      $(event.target).closest('.dropdown').toggleClass('open');
      return false;
    }
  },

  /**
   * This method is called when the value of the widget is changed by recommendation received from stack advisor api
   * @override
   * @method setValue
   */
   setValue: function() {
     this.setConfigValue({ context: this.get('config.value') });
   },

  /**
   * switch display of config to widget
   * @override
   * @method textBoxToWidget
   */
  textBoxToWidget: function() {
    this.setValue(this.get('config.value'));
    this.set("config.showAsTextBox", false);
  },

  /**
   * Returns <code>true</code> if raw value can be used by widget or widget view is activated.
   * @override
   * @returns {Boolean}
   */
  isWidgetViewAllowed: true,

  /**
   * Initialize widget with incompatible value as textbox
   * @override
   */
  initIncompatibleWidgetAsTextBox : function() {
    this.isValueCompatibleWithWidget();
  },


  isValueCompatibleWithWidget: function() {
    var res = this._super() && this.get('content.valuesList').someProperty('configValue', this.get('config.value'));
    if (!res) {
      this.updateWarningsForCompatibilityWithWidget(Em.I18n.t('config.infoMessage.wrong.value.for.combobox.widget').format(this.get('config.value')));
      return false;
    }
    this.updateWarningsForCompatibilityWithWidget('');
    return true;
  }

});
