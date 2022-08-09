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
require('views/common/controls_view');
/**
 * Common view for config widgets
 * @type {Em.View}
 */
App.ConfigWidgetView = Em.View.extend(App.SupportsDependentConfigs, App.WidgetPopoverSupport, App.ConvertUnitWidgetViewMixin, App.ServiceConfigCalculateId, {

  /**
   * @type {App.ConfigProperty}
   */
  config: null,

  /**
   * @type {App.ConfigProperty}
   */
  copyFromConfig: null,

  /**
   * Determines if user hover on widget-view
   * @type {boolean}
   */
  isHover: false,

  /**
   * Determines if widget controls should be disabled
   * @type {boolean}
   */
  disabled: false,

  /**
   * Determines if widget is editable
   * It true - show all control-elements (undo, override, finalize etc) for widget
   * If false - no widget control-elements will be shown
   * Bound from template
   * @type {boolean}
   */
  canEdit: true,

  canNotEdit: Em.computed.not('canEdit'),

  /**
   * Config label class attribute. Displays validation status of config.
   * @type {string}
   */
  configLabelClass: '',

  /**
   * defines if widget should be shown
   * if not, text-field with config value or label "Undefined" should be shown
   * @type {boolean}
   */
  doNotShowWidget: Em.computed.or('isPropertyUndefined', 'config.showAsTextBox'),

  /**
   * defines if property in not defined in selected version
   * in this case "Undefined" should be shown instead of widget
   * @type {boolean}
   */
  isPropertyUndefined: Em.computed.equal('config.value', 'Undefined'),

  /**
   * Tab where current widget placed
   * Bound in the template
   * @type {App.Tab}
   */
  tab: null,

  /**
   * Section where current widget placed
   * Bound in the template
   * @type {App.Section}
   */
  section: null,

  /**
   * Subsection where current widget placed
   * Bound in the template
   * @type {App.SubSection}
   */
  subSection: null,

  /**
   * Determines if user can switch custom widget-view to the input-field
   * @type {boolean}
   */
  supportSwitchToTextBox: false,

  /**
   * @type {boolean}
   */
  showPencil: Em.computed.and('supportSwitchToTextBox', '!disabled'),

  /**
   * Alias to <code>config.isOriginalSCP</code>
   * Should be used in the templates
   * Don't use original <code>config.isOriginalSCP</code> in the widget-templates!!!
   * @type {boolean}
   */
  isOriginalSCPBinding: 'config.isOriginalSCP',

  /**
   * Check if property validation failed for overridden property in case when its value is equal to parent
   * config property.
   * @type {boolean}
   */
  isOverrideEqualityError: function() {
    return this.get('config.parentSCP') && this.get('config.parentSCP.value') == this.get('config.value');
  }.property('config.isValid'),

  /**
   * Alias to <code>config.isComparison</code>
   * Should be used in the templates
   * Don't use original <code>config.isComparison</code> in the widget-templates!!!
   * @type {boolean}
   */
  isComparisonBinding: 'config.isComparison',

  classNameBindings:['isComparison:compare-mode', 'config.overrides.length:overridden-property'],

  issueMessage: '',

  issueView: Em.View.extend({

    tagName: 'i',

    classNames: ['glyphicon glyphicon-warning-sign'],

    classNameBindings: ['issueIconClass'],

    attributeBindings:['issueMessage:data-original-title'],

    /**
     * @type {App.ServiceConfigProperty}
     */
    config: null,

    /**
     * @type {string}
     */
    issueIconClass: '',

    /**
     * @type {string}
     */
    issueMessage: '',

    didInsertElement: function() {
      App.tooltip($(this.get('element')));
      this.errorLevelObserver();
      this.addObserver('issuedConfig.warnMessage', this, this.errorLevelObserver);
      this.addObserver('issuedConfig.errorMessage', this, this.errorLevelObserver);
      this.addObserver('parentView.isPropertyUndefined', this, this.errorLevelObserver);
    },

    willDestroyElement: function() {
      $(this.get('element')).tooltip('destroy');
      this.removeObserver('issuedConfig.warnMessage', this, this.errorLevelObserver);
      this.removeObserver('issuedConfig.errorMessage', this, this.errorLevelObserver);
      this.removeObserver('parentView.isPropertyUndefined', this, this.errorLevelObserver);
    },

    /**
     *
     * @method errorLevelObserver
     */
    errorLevelObserver: function() {
      var messageLevel = this.get('issuedConfig.errorMessage') ? 'ERROR': this.get('issuedConfig.warnMessage') ? 'WARN' : 'NONE';
      if (this.get('parentView.isPropertyUndefined')) {
        messageLevel = 'NONE';
      }
      var issue = {
        ERROR: {
          iconClass: '',
          message: this.get('issuedConfig.errorMessage'),
          configLabelClass: 'text-danger'
        },
        WARN: {
          iconClass: 'warning',
          message: this.get('issuedConfig.warnMessage'),
          configLabelClass: 'text-warning'
        },
        NONE: {
          iconClass: 'hide',
          message: false,
          configLabelClass: ''
        }
      }[messageLevel];
      this.set('parentView.configLabelClass', issue.configLabelClass);
      this.set('issueIconClass', issue.iconClass);
      this.set('issueMessage', issue.message);
      this.set('parentView.issueMessage', issue.message);
    },

    /**
     * @type {App.ServiceConfigProperty}
     */
    issuedConfig: function() {
      var config = this.get('config');
      // check editable override
      if (!config.get('isEditable') && config.get('isOriginalSCP') && config.get('overrides.length') && config.get('overrides').someProperty('isEditable', true)) {
        config = config.get('overrides').findProperty('isEditable', true);
      } else if (config.get('isOriginalSCP') && config.get('isEditable')) {
        // use original config if it is not valid
        if (!config.get('isValid')) {
          return config;
        // scan overrides for non valid values and use it
        } else if (config.get('overrides.length') && config.get('overrides').someProperty('isValid', false)) {
          return config.get('overrides').findProperty('isValid', false);
        }
      }
      return config;
    }.property('config.isEditable', 'config.overrides.length')

  }),

  /**
   * Config name to display.
   * @type {String}
   */
  configLabel: Em.computed.firstNotBlank('config.stackConfigProperty.displayName', 'config.displayName', 'config.name'),


  /**
   * Error message computed in config property model
   * @type {String}
   */
  configErrorMessageBinding: 'config.errorMessage',

  /**
   * Determines if config-value was changed
   * @type {boolean}
   */
  valueIsChanged: function () {
    return !Em.isNone(this.get('config.savedValue')) && this.get('config.value') != this.get('config.savedValue');
  }.property('config.value', 'config.savedValue'),

  /**
   * Enable/disable widget state
   * @method toggleWidgetState
   */
  toggleWidgetState: function () {
    this.set('disabled', !this.get('config.isEditable'));
  }.observes('config.isEditable'),

  /**
   * Reset config-value to its default
   * @method restoreValue
   */
  restoreValue: function () {
    var self = this;
    this.set('config.value', this.get('config.savedValue'));
    this.sendRequestRorDependentConfigs(this.get('config')).done(function() {
      self.restoreDependentConfigs(self.get('config'));
    });

    if (this.get('config.supportsFinal')) {
      this.get('config').set('isFinal', this.get('config.savedIsFinal'));
    }
    Em.$('body > .tooltip').remove();
  },

  /**
   * set <code>recommendedValue<code> to config
   * and send request to change dependent configs
   * @method setRecommendedValue
   */
  setRecommendedValue: function() {
    var self = this;
    this.set('config.value', this.get('config.recommendedValue'));
    this.sendRequestRorDependentConfigs(this.get('config')).done(function() {
      if (self.get('config.value') === self.get('config.savedValue')) {
        self.restoreDependentConfigs(self.get('config'));
      }
    });

    if (this.get('config.supportsFinal')) {
      this.get('config').set('isFinal', this.get('config.recommendedIsFinal'));
    }
    Em.$('body > .tooltip').remove();
  },

  /**
   * Determines if override is allowed for <code>config</code>
   * @type {boolean}
   */
  overrideAllowed: function () {
    var config = this.get('config');
    if (!config) return false;
    return config.get('isOriginalSCP') && config.get('isPropertyOverridable') && !this.get('config.isComparison');
  }.property('config.isOriginalSCP', 'config.isPropertyOverridable', 'config.isComparison'),

  /**
   * Determines if undo is allowed for <code>config</code>
   * @type {boolean}
   */
  undoAllowed: function () {
    var config = this.get('config');
    if (!config) return false;
    if (!this.get('isOriginalSCP') || this.get('disabled')) return false;
    return !config.get('cantBeUndone') && config.get('isNotDefaultValue');
  }.property('config.cantBeUndone', 'config.isNotDefaultValue', 'isOriginalSCP', 'disabled'),

  /**
   * Determines if "final"-button should be shown
   * @type {boolean}
   */
  showFinalConfig: function () {
    var config = this.get('config');
    return config.get('isFinal') || !config.get('isNotEditable');
  }.property('config.isFinal', 'config.isNotEditable'),

  /**
   *
   * @param {{context: App.ServiceConfigProperty}} event
   * @method toggleFinalFlag
   */
  toggleFinalFlag: function (event) {
    var configProperty = event.context;
    if (configProperty.get('isNotEditable')) {
      return;
    }
    configProperty.toggleProperty('isFinal');
  },

  /**
   * sync widget value with config value when dependent properties
   * have been loaded or changed
   * @method syncValueWithConfig
   */
  syncValueWithConfig: function() {
    this.setValue(this.get('config.value'));
  }.observes('controller.recommendationTimeStamp'),

  /**
   * defines if config has same config group as selected
   * @type {boolean}
   */
  referToSelectedGroup: function() {
    return this.get('controller.selectedConfigGroup.isDefault') && this.get('config.group') === null
    || this.get('controller.selectedConfigGroup.name') === this.get('config.group.name');
  }.property('controller.selectedConfigGroup.name', 'controller.selectedConfigGroup.isDefault'),

  didInsertElement: function () {
    App.tooltip(this.$('[data-toggle=tooltip]'), {placement: 'top'});
    App.tooltip($(this.get('element')).find('span'));
    var self = this;
    var element = this.$();
    if (element) {
      element.hover(function() {
        self.set('isHover', true);
      }, function() {
        self.set('isHover', false);
      });
    }
    this.initIncompatibleWidgetAsTextBox();
  },

  willInsertElement: function() {
    var configConditions = this.get('config.configConditions');
    var configAction = this.get('config.configAction');
    var isCopy =  this.get('config.copy');
    if (!Em.empty(isCopy)) {
      this.bindConfigValue();
    }

    if (configConditions && configConditions.length) {
      //Add Observer to configCondition that depends on another config value
      var isConditionConfigDependent = configConditions.filterProperty('resource', 'config').length;
      if (isConditionConfigDependent) {
        this.addObserver('config.value', this, this.configValueObserverForAttributes);
      }

      if (configAction) {
        this.addObserver('config.value', this, this.configValueObserverForAction);
      }
    }

  },

  willDestroyElement: function() {
    this.$('[data-toggle=tooltip]').tooltip('destroy');
    $(this.get('element')).find('span').tooltip('destroy');
    if (this.get('config.configConditions')) {
      this.removeObserver('config.value', this, this.configValueObserverForAttributes);
    }
    if (this.get('config.configAction')) {
      this.removeObserver('config.value', this, this.configValueObserverForAction);
    }
    if (this.get('copyFromConfig')) {
      this.removeObserver('copyFromConfig.value', this, this.configValueObserverForCopy);
    }
  },

  configValueObserverForAttributes: function() {
    const configConditions = this.get('config.configConditions'),
      serviceName = this.get('config.serviceName'),
      serviceConfigs = this.get('controller.stepConfigs').findProperty('serviceName',serviceName).get('configs');
    this.get('controller').updateAttributesFromConditions(configConditions, serviceConfigs, serviceName);
  },

  /**
   * This is an observer that is fired when a value of a config that is suppose to add/delete a component is changed
   * @private
   * @method {configValueObserverForAction}
   */
  configValueObserverForAction: function() {
    var assignMasterOnStep7Controller = App.router.get('assignMasterOnStep7Controller');
    var configAction = this.get('config.configAction');
    var serviceName = this.get('config.serviceName');
    var serviceConfigs = this.get('controller.stepConfigs').findProperty('serviceName', serviceName).get('configs');

    this.set('config.configActionComponent', null);
    var hostComponent = {
      componentName:configAction.get('componentName'),
      isClient: '',
      hostNames: [],
      action: ''
    };

    var hostComponentRecords = App.HostComponent.find().filterProperty('componentName', hostComponent.componentName);
    var stackComponentObj = App.StackServiceComponent.find(configAction.get('componentName'));

    if (stackComponentObj) {
      hostComponent.isClient = stackComponentObj.get('isClient');
    }

    if (hostComponentRecords.length) {
      hostComponent.hostNames = hostComponentRecords.mapProperty('hostName');
    }

    var isConditionTrue = App.configTheme.calculateConfigCondition(configAction.get("if"), serviceConfigs);
    var action = isConditionTrue ? configAction.get("then") : configAction.get("else");
    hostComponent.action = action;
    switch (action) {
      case 'add':
        // Disable save button until a host is selected
        var isComponentToBeInstalled = !App.HostComponent.find().someProperty('componentName', hostComponent.componentName);
        if (isComponentToBeInstalled) {
          this.set('controller.saveInProgress', true);
          assignMasterOnStep7Controller.execute(this, 'ADD', hostComponent);
        } else {
          if(hostComponent.componentName === "HIVE_SERVER_INTERACTIVE") {
            assignMasterOnStep7Controller.execute(this, 'ADD', hostComponent);
          }
          assignMasterOnStep7Controller.clearComponentsToBeDeleted(hostComponent.componentName);
        }
        break;
      case 'delete':
        assignMasterOnStep7Controller.execute(this, 'DELETE', hostComponent);
        break;
    }
  },

  bindConfigValue: function() {
    var serviceName = this.get('config.serviceName');
    var fileName =  this.get('config.copy').split('/')[0] + '.xml';
    var configName = this.get('config.copy').split('/')[1];

    var serviceConfigs = this.get('controller.stepConfigs').findProperty('serviceName', serviceName).get('configs');
    var config = serviceConfigs.filterProperty('filename',fileName).findProperty('name', configName);
    this.addObserver('copyFromConfig.value', this, this.configValueObserverForCopy);
    this.set('copyFromConfig',config);
  },

  configValueObserverForCopy: function() {
    var copyFromConfig = this.get('copyFromConfig');
    var displayName = copyFromConfig.get('displayName');
    var value = copyFromConfig.get('value');
    var description = copyFromConfig.get('description');
    var config = this.get('config');
    config.setProperties({
      value:value,
      initialValue:value,
      displayName:displayName,
      description:description
    });
  },

  /**
   * set widget value same as config value
   * useful for widgets that work with intermediate config value, not original
   * for now used in slider widget
   * @abstract
   */
  setValue: Em.K,

  /**
   * Config group bound property. Needed for correct render process in template.
   *
   * @returns {App.ConfigGroup|Boolean}
   */
  configGroup: function() {
    return !this.get('config.group') || this.get('config.group.isDefault') ? false : this.get('config.group');
  }.property('config.group.name'),

  /**
   * switcher to display config as widget or text field
   * @method toggleWidgetView
   */
  toggleWidgetView: function() {
    if (!this.get('isWidgetViewAllowed')) {
      return false;
    }
    if (this.get('config.showAsTextBox')) {
      this.textBoxToWidget();
    } else {
      this.widgetToTextBox();
    }
  },

  /**
   * switch display of config to text field
   * @method widgetToTextBox
   */
  widgetToTextBox: function() {
    this.set("config.showAsTextBox", true);
  },

  /**
   * switch display of config to widget
   * @method textBoxToWidget
   */
  textBoxToWidget: function() {
    if (this.isValueCompatibleWithWidget()) {
      this.setValue(this.get('config.value'));
      this.set("config.showAsTextBox", false);
    }
  },

  /**
   * check if config value can be converted to config widget value
   * IMPORTANT! Each config-widget that override this method should use <code>updateWarningsForCompatibilityWithWidget</code>
   * @returns {boolean}
   */
  isValueCompatibleWithWidget: function() {
    return this.get('isOverrideEqualityError') && !this.get('config.isValid') || this.get('config.isValid') || !this.get('supportSwitchToTextBox');
  },

  /**
   * Initialize widget with incompatible value as textbox
   */
  initIncompatibleWidgetAsTextBox : function() {
    this.get('config').set('showAsTextBox', !this.isValueCompatibleWithWidget());
  },

  /**
   * Returns <code>true</code> if raw value can be used by widget or widget view is activated.
   * @returns {Boolean}
   */
  isWidgetViewAllowed: function() {
    if (!this.get('config.showAsTextBox')) {
      return true;
    }
    return this.isValueCompatibleWithWidget();
  }.property('config.value', 'config.isFinal', 'config.showAsTextBox'),

  /**
   * Used in <code>isValueCompatibleWithWidget</code>
   * Updates issue-parameters if config is in the raw-mode
   * @param {string} message empty string if value compatible with widget, error-message if value isn't compatible with widget
   * @method updateWarningsForCompatibilityWithWidget
   */
  updateWarningsForCompatibilityWithWidget: function (message) {
    this.setProperties({
      warnMessage: message,
      'config.warnMessage': message,
      issueMessage: message,
      configLabelClass: message ? 'text-warning' : ''
    });
  }

});
