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
var validator = require('utils/validator');

var dbInfo = require('data/db_properties_info') || {};

var delay = (function(){
  var timer = 0;
  return function(callback, ms){
    clearTimeout (timer);
    timer = setTimeout(callback, ms);
  };
})();

/**
 * Abstract view for config fields.
 * Add popover support to control
 */
App.ServiceConfigPopoverSupport = Ember.Mixin.create({

  /**
   * Config object. It will instance of App.ServiceConfigProperty
   */
  serviceConfig: null,
  attributeBindings:['readOnly'],
  isPopoverEnabled: true,
  popoverPlacement: 'auto right',

  didInsertElement: function () {
    App.tooltip(this.$('[data-toggle=tooltip]'), {placement: 'top'});
    // if description for this serviceConfig not exist, then no need to show popover
    if (this.get('isPopoverEnabled') !== 'false' && this.get('serviceConfig.description')) {
      this.addPopover();
    }
  },

  /**
   * Element where popover is "appended"
   */
  elementForPopover: function () {
    return this.$();
  }.property(),

  /**
   * Add Popover for config-boxes
   *
   * @method addPopover
   */
  addPopover: function () {
    App.popover(this.get('elementForPopover'), {
      title: Em.I18n.t('installer.controls.serviceConfigPopover.title').format(
        this.get('serviceConfig.displayName'),
        (this.get('serviceConfig.displayName') === this.get('serviceConfig.name')) ? '' : this.get('serviceConfig.name')
      ),
      content: this.get('serviceConfig.description'),
      placement: this.get('popoverPlacement'),
      trigger: 'hover',
      html: true
    });
  },

  willDestroyElement: function() {
    this.$().popover('destroy');
  },

  readOnly: Em.computed.not('serviceConfig.isEditable')
});

App.SupportsDependentConfigs = Ember.Mixin.create({

  /**
   * method send request to check if some of dependent configs was changes
   * and in case there was changes shows popup with info about changed configs
   *
   * @param {App.ServiceConfigProperty} config
   * @param [controller]
   * @returns {$.Deferred}
   */
  sendRequestRorDependentConfigs: function(config, controller) {
    if (!config || (!config.get('isValid') && config.get('isNotDefaultValue'))) return $.Deferred().resolve().promise();
    controller = controller || this.get('controller');
    if (controller && ['mainServiceInfoConfigsController','wizardStep7Controller'].contains(controller.get('name'))) {
      var name = config.get('name');
      var saveRecommended = (config.get('value') === config.get('recommendedValue'));
      var type = App.config.getConfigTagFromFileName(config.get('filename'));
      var p = App.configsCollection.getConfig(App.config.configId(name, type));
      controller.removeCurrentFromDependentList(config, saveRecommended);
       if ((p && Em.get(p, 'propertyDependedBy.length') > 0)
         || (config.get('displayType') === 'user' && config.get('oldValue') !== config.get('value'))) {
         var old = config.get('oldValue');
         config.set('oldValue', config.get('value'));
         return controller.loadConfigRecommendations([{
           "type": type,
           "name": name,
           "old_value": Em.isNone(old) ? config.get('initialValue') : old
         }]);
      }
    }
    return $.Deferred().resolve().promise();
  },

  /**
   * Restore values for dependent configs by parent config info.
   * NOTE: If dependent config inherited from multiply configs its
   * value will be restored only when all parent configs are being restored.
   *
   * @param {App.ServiceConfigProperty} parentConfig
   */
  restoreDependentConfigs: function(parentConfig) {
    var controller = this.get('controller');
    var dependentConfigs = controller.get('recommendations');
    try {
      controller.set('recommendations', dependentConfigs.reject(function(item) {
        if (item.parentConfigs.contains(parentConfig.get('name'))) {
          if (item.parentConfigs.length > 1) {
            item.parentConfigs.removeObject(parentConfig.get('name'));
          } else {
            // reset property value
            var property = App.config.findConfigProperty(controller.get('stepConfigs'), item.propertyName, App.config.getOriginalFileName(item.fileName));
            if (property) {
              property.set('value', property.get('savedValue') || property.get('initialValue'));
            }
            return true;
          }
        }
        return false;
      }));
    } catch(e) {
      console.warn('Dependent properties popup was not cleared');
    }
  }

});

/**
 * mixin is used to send request for recommendations
 * when config value is updated by user
 */
App.ValueObserver = Em.Mixin.create(App.SupportsDependentConfigs, {

  selected: false,

  focusOut: function () {
    this.set('selected', false);
  },

  focusIn: function () {
    this.set('selected', true);
  },

  onValueUpdate: function () {
    if (!this.get('isVisible')) return;
    if (this.get('selected') || this.get('serviceConfig.changedViaUndoValue')) {
      var self = this, config = this.get('serviceConfig'),
        controller = this.get('controller');
      Em.set(config, 'didUserOverrideValue', true);
      delay(function(){
        self.sendRequestRorDependentConfigs(config, controller);
      }, 500);
    }
  }.observes('serviceConfig.value')
});

/**
 * mixin is used to send request for recommendations
 * when config value is updated by user
 */
App.WidgetValueObserver = Em.Mixin.create(App.ValueObserver, {
  onValueUpdate: function () {
    if (this.get('selected')) {
      var self = this, config = this.get('config'),
        controller = this.get('controller');
      Em.set(config, 'didUserOverrideValue', true);
      delay(function(){
        self.sendRequestRorDependentConfigs(config, controller);
      }, 500);
    }
  }.observes('config.value')
});

/**
 * mixin set class that serve as unique element identifier,
 * id not used in order to avoid collision with ember ids
 */
App.ServiceConfigCalculateId = Ember.Mixin.create({
  'data-qa': Ember.computed(function () {
    const config = this.get('config') && this.get('config.widget') ? this.get('config') : this.get('serviceConfig') || {};
    const configName = Em.get(config, 'name') || '';
    if (configName === 'content') {
      return Em.get(config, 'id') || '';
    }
    return configName;
  })
});

/**
 * Default input control
 * @type {*}
 */
App.ServiceConfigTextField = Ember.TextField.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.ValueObserver, {
  classNames: ['form-control'],
  valueBinding: 'serviceConfig.value',
  classNameBindings: 'textFieldClassName',
  placeholderBinding: 'serviceConfig.placeholder',

  //Set editDone true for last edited config text field parameter
  focusOut: function () {
    this._super();
    this.get('serviceConfig').set("editDone", true);
  },
  //Set editDone false for all current category config text field parameter
  focusIn: function () {
    this._super();
    if (!this.get('serviceConfig.isOverridden') && !this.get('serviceConfig.isComparison')) {
      if (this.get('parentView.categoryConfigsAll')) {
        this.get("parentView.categoryConfigsAll").setEach("editDone", false);
      }
    }
  },

  textFieldClassName: function () {
    if (this.get('serviceConfig.unit')) {
      return ['input-sm'];
    } else if (this.get('serviceConfig.displayType') === 'principal') {
      return ['col-md-12'];
    } else {
      return ['col-md-9 long-input'];
    }
  }.property('serviceConfig.displayType', 'serviceConfig.unit')

});

/**
 * Customized input control for user/group configs with corresponding uid/gid specified
 * @type {Em.View}
 */
App.ServiceConfigTextFieldUserGroupWithID = Ember.View.extend(App.ServiceConfigPopoverSupport, {
  valueBinding: 'serviceConfig.value',
  placeholderBinding: 'serviceConfig.savedValue',
  classNamesBindings: 'view.fullWidth::display-inline-block',
  isPopoverEnabled: 'false',

  fullWidth: false,

  templateName: require('templates/wizard/controls_service_config_usergroup_with_id'),

  isUIDGIDVisible: function () {
    var serviceConfigs = this.get('parentView').serviceConfigs;
    var overrideUid = serviceConfigs && serviceConfigs.findProperty('name', 'override_uid');
    //don't display the ugid field if there is no uid/gid for this property or override_uid is unchecked
    if (Em.isNone(this.get('serviceConfig.ugid')) || overrideUid && overrideUid.value === 'false') {
      return false;
    }

    var serviceName = this.get('serviceConfig').name.substr(0, this.get('serviceConfig').name.indexOf('_')).toUpperCase();
    if (serviceName === 'ZK') {
      serviceName = 'ZOOKEEPER';
    }
    if (serviceName === 'MAPRED') {
      serviceName = 'YARN';
    }
    //addServiceController and service already installed or Hadoop user group
    if (App.Service.find(serviceName).get('isLoaded') || serviceName === 'USER') {
      return false;
    }

    return this.get('parentView.isUIDGIDVisible');
  }.property('parentView.isUIDGIDVisible')
});

/**
 * Customized input control with Units type specified
 * @type {Em.View}
 */
App.ServiceConfigTextFieldWithUnit = Ember.View.extend(App.ServiceConfigPopoverSupport, App.ValueObserver, {
  valueBinding: 'serviceConfig.value',
  classNames: ['input-group', 'with-unit', 'col-md-4'],
  placeholderBinding: 'serviceConfig.savedValue',

  templateName: require('templates/wizard/controls_service_config_textfield_with_unit')
});

/**
 * Password control
 * @type {*}
 */
App.ServiceConfigPasswordField = Ember.View.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, {

  serviceConfig: null,

  placeholder: Em.I18n.t('form.item.placeholders.typePassword'),

  templateName: require('templates/common/configs/widgets/service_config_password_field'),

  classNames: ['password-field-wrapper'],

  readOnly: Em.computed.not('serviceConfig.isEditable'),

  keyPress: function (event) {
    if (event.keyCode == 13) {
      return false;
    }
  }

});

/**
 * Textarea control
 * @type {*}
 */
App.ServiceConfigTextArea = Ember.TextArea.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.ValueObserver, {

  valueBinding: 'serviceConfig.value',
  rows: 4,
  classNames: ['directories', 'form-control'],
  classNameBindings: ['widthClass'],
  widthClass: 'col-md-9'
});


/**
 * Special config type for Capacity Scheduler
 */
App.CapacitySceduler = App.ServiceConfigTextArea.extend({

  rows: 16,

  /**
   * specific property handling for cs
   *
   * @param {App.ServiceConfigProperty} config
   * @param [controller]
   * @returns {$.Deferred}
   * @override
   */
  sendRequestRorDependentConfigs: function(config, controller) {
    if (!config.get('isValid') && config.get('isNotDefaultValue')) return $.Deferred().resolve().promise();
    controller = controller || this.get('controller');
    if (controller && ['mainServiceInfoConfigsController','wizardStep7Controller'].contains(controller.get('name'))) {
      var schedulerConfigs = config.get('value').split('\n').map(function (_property) {
        return {
          "type": 'capacity-scheduler',
          "name": _property.split('=')[0]
        }
      });
      if (!schedulerConfigs.someProperty('name', 'capacity-scheduler')) {
        schedulerConfigs.push({
          "type": 'capacity-scheduler',
          "name": 'capacity-scheduler'
        });
      }
      return controller.loadConfigRecommendations(schedulerConfigs);
    }

    return $.Deferred().resolve().promise();
  }

});

/**
 * Textarea control for content type
 * @type {*}
 */
App.ServiceConfigTextAreaContent = Ember.TextArea.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.ValueObserver, {

  valueBinding: 'serviceConfig.value',
  rows: 20,
  classNames: ['col-md-10', 'form-control']
});

/**
 * Textarea control with bigger height
 * @type {*}
 */
App.ServiceConfigBigTextArea = App.ServiceConfigTextArea.extend(App.ServiceConfigCalculateId, {
  rows: 10
});

var checkboxConfigView = Ember.Checkbox.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.SupportsDependentConfigs, {
  allowedPairs: {
    'trueFalse': ["true", "false"],
    'YesNo': ["Yes", "No"],
    'YESNO': ["YES", "NO"],
    'yesNo': ["yes", "no"]
  },

  trueValue: true,
  falseValue: false,

  checked: false,

  elementForPopover: function () {
    return this.$().next();
  }.property(),

  /**
   * set appropriate config values pair
   * to define which value is positive (checked) property
   * and what value is negative (unchecked) property
   */
  didInsertElement: function() {
    var self = this;
    if (this.get('serviceConfig').isDestroyed) return;
    this._super();
    this.addObserver('serviceConfig.value', this, 'toggleChecker');
    var isInverted = this.get('serviceConfig.displayType') === 'boolean-inverted';
    Object.keys(this.get('allowedPairs')).forEach(function(key) {
      if (this.get('allowedPairs')[key].contains(this.get('serviceConfig.value'))) {
        this.set('trueValue', isInverted ? this.get('allowedPairs')[key][1] :this.get('allowedPairs')[key][0]);
        this.set('falseValue', isInverted ? this.get('allowedPairs')[key][0] :this.get('allowedPairs')[key][1]);
      }
    }, this);
    this.set('checked', this.get('serviceConfig.value') === this.get('trueValue'));
    this.propertyDidChange('checked');
    Em.run.next(function () {
      if (self.$()) {
        self.propertyDidChange('elementForPopover');
        self.addPopover();
      }
    });
  },

  willDestroyElement: function() {
    this.removeObserver('serviceConfig.value', this, 'checkedBinding');
  },

  /***
   * defines if checkbox value appropriate to the config value
   * @returns {boolean}
   */
  isNotAppropriateValue: function() {
    return this.get('serviceConfig.value') !== this.get(this.get('checked') + 'Value');
  },

  /**
   * change service config value if click on checkbox
   */
  toggleValue: function() {
    if (this.isNotAppropriateValue()){
      this.set('serviceConfig.value', this.get(this.get('checked') + 'Value'));
      this.get('serviceConfig').set("editDone", true);
      this.sendRequestRorDependentConfigs(this.get('serviceConfig'));

      //if the checkbox being toggled is the 'Have Ambari manage UIDs' in Misc Tab, show/hide uid/gid column accordingly
      if (this.get('serviceConfig.name') === 'override_uid') {
         this.set('parentView.isUIDGIDVisible', this.get('checked'));
      }
    }
  }.observes('checked'),

  /**
   * change checkbox value if click on undo
   */
  toggleChecker: function() {
    if (this.isNotAppropriateValue()) {
      this.set('checked', !this.get('checked'));
    }
  },

  disabled: Em.computed.not('serviceConfig.isEditable'),

  disabledDidChange: function() {
    if (!this.get('checkboxInstance')) return;
  }.observes('disabled'),

  //Set editDone false for all current category config text field parameter
  focusIn: function (event) {
    if (!this.get('serviceConfig.isOverridden') && !this.get('serviceConfig.isComparison')) {
      this.get("parentView.categoryConfigsAll").setEach("editDone", false);
    }
  }
});

/**
 * Checkbox control
 * @type {*}
 */
App.ServiceConfigCheckbox = App.CheckboxView.extend({
  classNames: ['display-inline-block'],
  classNameBindings: ['containerClassName'],
  containerClassName: 'checkbox',
  categoryConfigsAllBinding: 'parentView.categoryConfigsAll',
  checkboxView: checkboxConfigView.extend({
    serviceConfigBinding: 'parentView.serviceConfig',
    didInsertElement: function() {
      this.set('parentView.checkboxId', this.get('elementId'));
      this._super();
    }
  })
});

/**
 * Checkbox control which can hide or show dependent  properties
 * @type {*|void}
 */
App.ServiceConfigCheckboxWithDependencies = App.ServiceConfigCheckbox.extend({

  toggleDependentConfigs: function() {
    if (this.get('serviceConfig.dependentConfigPattern')) {
      if (this.get('serviceConfig.dependentConfigPattern') === "CATEGORY") {
        this.disableEnableCategoryConfigs();
      } else {
        this.showHideDependentConfigs();
      }
    }
  }.observes('checked'),

  disableEnableCategoryConfigs: function () {
    this.get('categoryConfigsAll').setEach('isEditable', this.get('checked'));
    this.set('serviceConfig.isEditable', true);
  },

  showHideDependentConfigs: function () {
    this.get('categoryConfigsAll').forEach(function (c) {
      if (c.get('name').match(this.get('serviceConfig.dependentConfigPattern')) && c.get('name') != this.get('serviceConfig.name'))
        c.set('isVisible', this.get('checked'))
    }, this);
  }
});

App.ServiceConfigRadioButtons = Ember.View.extend(App.ServiceConfigCalculateId, App.SupportsDependentConfigs, {
  templateName: require('templates/wizard/controls_service_config_radio_buttons'),

  didInsertElement: function () {
    // on page render, automatically populate JDBC URLs only for default database settings
    // so as to not lose the user's customizations on these fields
    if (this.get('hostNameProperty')) {
      this.get('hostNameProperty').set('isEditable', !this.get('isNewDb'));
    }
    if (['addServiceController', 'installerController'].contains(this.get('controller.wizardController.name')) && !App.StackService.find(this.get('serviceConfig.serviceName')).get('isInstalled')) {
      if (this.get('isNewDb') || this.get('dontUseHandleDbConnection').contains(this.get('serviceConfig.name'))) {
        this.onOptionsChange();
      } else {
        if ((App.get('isHadoopWindowsStack') && this.get('inMSSQLWithIA')) || this.get('serviceConfig.name') === 'DB_FLAVOR') {
          this.onOptionsChange();
        }
      }
      this.handleDBConnectionProperty();
    }
  },

  /**
   * Radio buttons that are not DB options and should not trigger any observer or change any other property's value
   * Ranger service -> "Authentication method" property is an example for non DB related radio button
   */
  nonDBRadioButtons: function() {
    return this.get('dontUseHandleDbConnection').without('DB_FLAVOR');
  }.property('dontUseHandleDbConnection'),

  /**
   * properties with these names don'use handleDBConnectionProperty method
   */
  dontUseHandleDbConnection: function () {
    // functionality added in Ranger 0.5
    // remove DB_FLAVOR so it can handle DB Connection checks
    var rangerService = App.StackService.find().findProperty('serviceName', 'RANGER');
    var rangerVersion = rangerService ? rangerService.get('serviceVersion') : '';
    if (rangerVersion && rangerVersion.split('.')[0] < 1 && rangerVersion.split('.')[1] < 5) {
      return ['DB_FLAVOR', 'authentication_method'];
    }
    return ['ranger.authentication.method'];
  }.property('App.currentStackName'),

  serviceConfig: null,
  categoryConfigsAll: null,

  /**
   * defines if new db is selected;
   * @type {boolean}
   */
  isNewDb: function() {
    return /New /g.test(this.get('serviceConfig.value'));
  }.property('serviceConfig.serviceName', 'serviceConfig.value'),

  /**
   * defines if 'Existing MSSQL Server database with integrated authentication' is selected
   * in this case some properties can have different behaviour
   * @type {boolean}
   */
  inMSSQLWithIA: Em.computed.equal('serviceConfig.value', 'Existing MSSQL Server database with integrated authentication'),

  /**
   * Radio button has very uncomfortable values for managing it's state
   * so it's better to use code values that easier to manipulate. Ex:
   * "Existing MySQL Database" transforms to "MYSQL"
   * @type {string}
   */
  getDbTypeFromRadioValue: function() {
    var currentValue = this.get('serviceConfig.value');
    /** TODO: Remove SQLA from the list of databases once Ranger DB_FLAVOR=SQLA is replaced with SQL Anywhere */
    var databases = /MySQL|Postgres|Oracle|Derby|MSSQL|SQLA|Anywhere/gi;
    if (this.get('inMSSQLWithIA')) {
      return 'MSSQL2';
    } else {
      var matches = currentValue.match(databases);
      if (matches) {
        return currentValue.match(databases)[0].toUpperCase();
      } else {
        return "MYSQL";
      }
    }
  }.property('serviceConfig.serviceName', 'serviceConfig.value'),

  onOptionsChange: function () {
    if (!this.get('nonDBRadioButtons').contains(this.get('serviceConfig.name'))) {
      /** if new db is selected host name must be same as master of selected service (and can't be changed)**/
      this.handleSpecialUserPassProperties();
    }
  }.observes('databaseProperty.value', 'hostNameProperty.value', 'serviceConfig.value'),

  name: function () {
    var name = this.get('serviceConfig.radioName');
    if (!this.get('serviceConfig.isOriginalSCP')) {
      if (this.get('serviceConfig.isComparison')) {
        var version = this.get('serviceConfig.compareConfigs') ? this.get('controller.selectedVersion') : this.get('version');
        name += '-v' + version;
      } else {
        var group = this.get('serviceConfig.group.name');
        name += '-' + group;
      }
    }
    return name;
  }.property('serviceConfig.radioName'),

  /**
   * Just property object for database name
   * @type {App.ServiceConfigProperty}
   */
  databaseProperty: function () {
    return this.getPropertyByType('db_name');
  }.property('serviceConfig.serviceName'),

  /**
   * Just property object for host name
   * @type {App.ServiceConfigProperty}
   */
  hostNameProperty: function () {
    var host = this.getPropertyByType('host_name');
    if (host && !host.get('value')) {
      if (host.get('savedValue')) {
        host.set('value', host.get('savedValue'));
      } else if (host.get('recommendedValue')) {
        host.set('value', host.get('recommendedValue'));
      }
    }
    return host;
  }.property('serviceConfig.serviceName', 'serviceConfig.value'),

  /**
   * Just property object for database name
   * @type {App.ServiceConfigProperty}
   */
  userProperty: function () {
    return this.getPropertyByType('user_name');
  }.property('serviceConfig.serviceName'),

  /**
   * Just property object for database name
   * @type {App.ServiceConfigProperty}
   */
  passwordProperty: function () {
    return this.getPropertyByType('password');
  }.property('serviceConfig.serviceName'),

  /**
   *
   * @param propertyType
   * @returns {*}
   */
  getDefaultPropertyValue: function(propertyType) {
    var dbProperties = dbInfo.dpPropertiesMap[this.get('getDbTypeFromRadioValue')],
      serviceName = this.get('serviceConfig.serviceName');
    return dbProperties[serviceName] && dbProperties[serviceName][propertyType]
      ? dbProperties[serviceName][propertyType] : dbProperties[propertyType];
  },

  /**
   *
   * @param propertyType
   * @returns {*|Object}
   */
  getPropertyByType: function(propertyType) {
    if (dbInfo.dpPropertiesByServiceMap[this.get('serviceConfig.serviceName')]) {
      //@TODO: dbInfo.dpPropertiesByServiceMap has corresponding property name but does not have filenames with it. this can cause issue when there are multiple db properties with same name belonging to different files
      /** check if selected service has db properties**/
      return this.get('controller.selectedService.configs').findProperty('name', dbInfo.dpPropertiesByServiceMap[this.get('serviceConfig.serviceName')][propertyType]);
    }
    return null;
  },

  /**
   * This method hides properties <code>user_name<code> and <code>password<code> in case selected db is
   * "Existing MSSQL Server database with integrated authentication" or similar
   * @method handleSpecialUserPassProperties
   */
  handleSpecialUserPassProperties: function() {
    ['user_name', 'password'].forEach(function(pType) {
      var property = this.getPropertyByType(pType);
      if (property) {
        property.setProperties({
          'isVisible': !this.get('inMSSQLWithIA'),
          'isRequired': !this.get('inMSSQLWithIA')
        });
      }
    }, this);
  },

  /**
   * `Observer` that add <code>additionalView</code> to <code>App.ServiceConfigProperty</code>
   * that responsible for (if existing db selected)
   * 1. checking database connection
   * 2. showing jdbc driver setup warning msg.
   *
   * @method handleDBConnectionProperty
   **/
  handleDBConnectionProperty: function() {
    if (this.get('dontUseHandleDbConnection').contains(this.get('serviceConfig.name'))) {
      return;
    }
    var handledProperties = ['oozie_database', 'hive_database', 'DB_FLAVOR'];
    var currentValue = this.get('serviceConfig.value');
    /** TODO: Remove SQLA from the list of databases once Ranger DB_FLAVOR=SQLA is replaced with SQL Anywhere */
    var databases = /MySQL|PostgreSQL|Postgres|Oracle|Derby|MSSQL|SQLA|Anywhere/gi;
    var currentDB = currentValue.match(databases)[0];
    /** TODO: Remove SQLA from the list of databases once Ranger DB_FLAVOR=SQLA is replaced with SQL Anywhere */
    var checkDatabase = /existing/gi.test(currentValue);
    // db connection check button show up if existed db selected
    var propertyAppendTo1 = this.get('categoryConfigsAll').findProperty('displayName', 'Database URL');
    // warning msg under database type radio buttons, to warn the user to setup jdbc driver if existed db selected
    var propertyHive = this.get('categoryConfigsAll').findProperty('displayName', 'Hive Database');
    var propertyOozie = this.get('categoryConfigsAll').findProperty('displayName', 'Oozie Database');
    var propertyAppendTo2 = propertyHive ? propertyHive : propertyOozie;
    // RANGER specific
    if (this.get('serviceConfig.serviceName') === 'RANGER') {
      propertyAppendTo1 = this.get('categoryConfigsAll').findProperty('name', 'ranger.jpa.jdbc.url');
      propertyAppendTo2 = this.get('categoryConfigsAll').findProperty('name', 'DB_FLAVOR');
      // check for all db types when installing Ranger - not only for existing ones
      checkDatabase = true;
    }
    // Hive specific
    if (this.get('serviceConfig.serviceName') === 'HIVE') {
      // check for all db types when installing Hive - not only for existing ones
      checkDatabase = true;
    }
    if (propertyAppendTo1) {
      propertyAppendTo1.set('additionalView', null);
    }
    if (propertyAppendTo2) {
      propertyAppendTo2.set('additionalView', null);
    }
    var shouldAdditionalViewsBeSet = currentDB && checkDatabase && handledProperties.contains(this.get('serviceConfig.name')),
      driver = this.getDefaultPropertyValue('sql_jar_connector') ? this.getDefaultPropertyValue('sql_jar_connector').split("/").pop() : 'driver.jar',
      dbType = this.getDefaultPropertyValue('db_type'),
      dbName = this.getDefaultPropertyValue('db_name'),
      driverName = this.getDefaultPropertyValue('driver_name'),
      driverDownloadUrl = this.getDefaultPropertyValue('driver_download_url'),
      additionalView1 = shouldAdditionalViewsBeSet && !this.get('isNewDb') ? App.CheckDBConnectionView.extend({databaseName: dbType}) : null,
      additionalView2 = shouldAdditionalViewsBeSet ? Ember.View.extend({
        template: Ember.Handlebars.compile('<div class="alert alert-warning">{{{view.message}}}</div>'),
        message: function() {
          return Em.I18n.t('services.service.config.database.msg.jdbcSetup.detailed').format(dbName, dbType, driver, driverDownloadUrl, driverName);
        }.property()
      }) : null;
    if (propertyAppendTo1) {
      Em.run.next(function () {
        propertyAppendTo1.set('additionalView', additionalView1);
      });
    }
    if (propertyAppendTo2) {
      Em.run.next(function () {
        propertyAppendTo2.set('additionalView', additionalView2);
      });
    }
  }.observes('serviceConfig.value'),

  options: function () {
    return this.get('serviceConfig.options').map(function (option) {
      var dbTypePattern = /mysql|postgres|oracle|derby|mssql|sql\s?a/i,
        className = '',
        displayName = Em.get(option, 'displayName'),
        dbTypeMatch = displayName.match(dbTypePattern);
      if (dbTypeMatch) {
        var dbSourcePattern = /new/i,
          newDbMatch = displayName.match(dbSourcePattern);
        if (newDbMatch) {
          className += 'new-';
        }
        className += dbTypeMatch[0].replace(' ', '').toLowerCase();
      }
      return className ? Em.Object.create(option, {
        className: className,
        radioId: ''
      }) : option;
    });
  }.property('serviceConfig.options')
});

App.ServiceConfigRadioButton = Ember.Checkbox.extend(App.SupportsDependentConfigs, {
  tagName: 'input',
  attributeBindings: ['type', 'name', 'value', 'checked', 'disabled'],
  checked: false,
  clicked: false,
  type: 'radio',
  name: null,
  value: null,
  /**
   * Element id passed to label's <code>for</code> attribute.
   * @type {String}
   */
  radioId: '',

  didInsertElement: function () {
    this.set('radioId', this.get('elementId'));
    this.set('checked', this.get('parentView.serviceConfig.value') === this.get('value'));
  },

  click: function () {
    this.set('clicked', true);
    this.set('checked', true);
    this.onChecked();
  },

  onChecked: function () {
    // Wrapping the call with Ember.run.next prevents a problem where setting isVisible on component
    // causes JS error due to re-rendering.  For example, this occurs when switching the Config Group
    // in Service Config page
    if (this.get('clicked')) {
      Em.run.next(this, function() {
        this.set('parentView.serviceConfig.value', this.get('value'));
        this.sendRequestRorDependentConfigs(this.get('parentView.serviceConfig'));
        this.set('clicked', false);
        this.updateForeignKeys();
      });
    }
  }.observes('checked'),

  updateCheck: function() {
    if (!this.get('clicked')) {
      this.set('checked', this.get('parentView.serviceConfig.value') === this.get('value'));
      this.updateForeignKeys();
    }
  }.observes('parentView.serviceConfig.value'),

  updateForeignKeys: function() {
    var components = this.get('parentView.serviceConfig.options');
    if (components && components.someProperty('foreignKeys')) {
      this.get('controller.stepConfigs').findProperty('serviceName', this.get('parentView.serviceConfig.serviceName')).propertyDidChange('errorCount');
    }
  },


  disabled: function () {
    return !this.get('parentView.serviceConfig.isEditable') ||
      !['addServiceController', 'installerController'].contains(this.get('controller.wizardController.name')) && /^New\s\w+\sDatabase$/.test(this.get('value'));
  }.property('parentView.serviceConfig.isEditable')
});

App.ServiceConfigComboBox = Ember.Select.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.SupportsDependentConfigs, {
  contentBinding: 'serviceConfig.options',
  selectionBinding: 'serviceConfig.value',
  placeholderBinding: 'serviceConfig.savedValue',
  classNames: [ 'col-md-3' ]
});


/**
 * Base component for host config with popover support
 */
App.ServiceConfigHostPopoverSupport = Ember.Mixin.create({

  /**
   * Config object. It will instance of App.ServiceConfigProperty
   */
  serviceConfig: null,

  didInsertElement: function () {
    App.popover(this.$(), {
      title: this.get('serviceConfig.displayName'),
      content: this.get('serviceConfig.description'),
      placement: 'right',
      trigger: 'hover'
    });
  }
});

/**
 * Master host component.
 * Show hostname without ability to edit it
 * @type {*}
 */
App.ServiceConfigMasterHostView = Ember.View.extend(App.ServiceConfigHostPopoverSupport, App.ServiceConfigCalculateId, {

  classNames: ['master-host', 'col-md-6'],
  valueBinding: 'serviceConfig.value',

  template: Ember.Handlebars.compile('{{view.value}}')

});

/**
 * text field property view that enables possibility
 * for check connection
 * @type {*}
 */
App.checkConnectionView = App.ServiceConfigTextField.extend({
  didInsertElement: function() {
    this._super();
    if (this.get('controller.isHostsConfigsPage')) {
      return;
    }
    var kdc = this.get('categoryConfigsAll').findProperty('name', 'kdc_type');
    var propertyAppendTo = this.get('categoryConfigsAll').findProperty('name', 'domains');
    if (propertyAppendTo) {
      try {
        propertyAppendTo.set('additionalView', App.CheckDBConnectionView.extend({databaseName: kdc && kdc.get('value')}));
      } catch (e) {
        console.error('error while adding "Test connection button"');
      }
    }
  }
});

/**
 * Show value as plain label in italics
 * @type {*}
 */
App.ServiceConfigLabelView = Ember.View.extend(App.ServiceConfigHostPopoverSupport, App.ServiceConfigCalculateId, {

  classNames: ['master-host', 'col-md-6'],
  valueBinding: 'serviceConfig.value',
  unitBinding: 'serviceConfig.unit',

  fullValue: function () {
    var value = this.get('value');
    var unit = this.get('unit');
    return unit ? value + ' ' + unit : value;
  }.property('value', 'unit'),

  template: Ember.Handlebars.compile('<i>{{formatWordBreak view.fullValue}}</i>')
});

/**
 * Base component to display Multiple hosts
 * @type {*}
 */
App.ServiceConfigMultipleHostsDisplay = Ember.Mixin.create(App.ServiceConfigHostPopoverSupport, App.ServiceConfigCalculateId, {

  hasNoHosts: function () {
    if (!this.get('value')) {
      return true;
    }
    return !this.get('value.length');
  }.property('value'),

  formatValue: Em.computed.ifThenElseByKeys('hasOneHost', 'value.firstObject', 'value'),

  hasOneHost: Em.computed.equal('value.length', 1),

  hasMultipleHosts: Em.computed.gt('value.length', 1),

  otherLength: function () {
    var len = this.get('value.length');
    if (len > 2) {
      return Em.I18n.t('installer.controls.serviceConfigMultipleHosts.others').format(len - 1);
    }
    return Em.I18n.t('installer.controls.serviceConfigMultipleHosts.other');
  }.property('value')

});

/**
 * Multiple Slave Hosts component
 * @type {*}
 */
App.ServiceConfigComponentHostsView = Ember.View.extend(App.ServiceConfigMultipleHostsDisplay, App.ServiceConfigCalculateId, {

  viewName: 'serviceConfigSlaveHostsView',

  classNames: ['component-hosts', 'form-text'],

  valueBinding: 'serviceConfig.value',

  templateName: require('templates/wizard/component_hosts'),

  firstHost: Em.computed.firstNotBlank('value.firstObject', 'serviceConfig.value.firstObject'),

  /**
   * Onclick handler for link
   */
  showHosts: function () {
    var serviceConfig = this.get('serviceConfig');
    App.ModalPopup.show({
      header: Em.I18n.t('installer.controls.serviceConfigMasterHosts.header').format(serviceConfig.category),
      bodyClass: Ember.View.extend({
        serviceConfig: serviceConfig,
        templateName: require('templates/wizard/component_hosts_popup')
      }),
      secondary: null
    });
  }

});


/**
 * DropDown component for <code>select hosts for groups</code> popup
 * @type {*}
 */
App.SlaveComponentDropDownGroupView = Ember.View.extend(App.ServiceConfigCalculateId, {

  viewName: "slaveComponentDropDownGroupView",

  /**
   * On change handler for <code>select hosts for groups</code> popup
   * @param event
   */
  changeGroup: function (event) {
    var host = this.get('content');
    var groupName = $('#' + this.get('elementId') + ' select').val();
    this.get('controller').changeHostGroup(host, groupName);
  },

  optionTag: Ember.View.extend({

    /**
     * Whether current value(OptionTag value) equals to host value(assigned to SlaveComponentDropDownGroupView.content)
     */
    selected: function () {
      return this.get('parentView.content.group') === this.get('content');
    }.property('content')
  })
});

/**
 * View for testing connection to database.
 **/
App.CheckDBConnectionView = Ember.View.extend({
  templateName: require('templates/common/form/check_db_connection'),
  /** @property {string} btnCaption - text for button **/
  btnCaption: function() {
    return this.get('parentView.service.serviceName') === 'KERBEROS'
      ? Em.I18n.t('services.service.config.kdc.btn.idle')
      : Em.I18n.t('services.service.config.database.btn.idle')
  }.property('parentView.service.serviceName'),
  /** @property {string} responseCaption - text for status link **/
  responseCaption: null,
  /** @property {boolean} isConnecting - is request to server activated **/
  isConnecting: false,
  /** @property {boolean} isValidationPassed - check validation for required fields **/
  isValidationPassed: null,
  /** @property {string} databaseName- name of current database **/
  databaseName: null,
  /** @property {boolean} isRequestResolved - check for finished request to server **/
  isRequestResolved: false,
  /** @property {boolean} isConnectionSuccess - check for successful connection to database **/
  isConnectionSuccess: null,
  /** @property {string} responseFromServer - message from server response **/
  responseFromServer: null,
  /** @property {Object} ambariRequiredProperties - properties that need for custom action request **/
  ambariRequiredProperties: null,
  /** @property {Number} currentRequestId - current custom action request id **/
  currentRequestId: null,
  /** @property {Number} currentTaskId - current custom action task id **/
  currentTaskId: null,
  /** @property {jQuery.Deferred} request - current $.ajax request **/
  request: null,
  /** @property {Number} pollInterval - timeout interval for ajax polling **/
  pollInterval: 3000,
  /** @property {Object} logsPopup - popup with DB connection check info **/
  logsPopup: null,
  /** @property {string} hostNameProperty - host name property based on service and database names **/
  hostNameProperty: function() {
    if (!/wizard/i.test(this.get('controller.name')) && this.get('parentView.service.serviceName') === 'HIVE') {
      return this.get('parentView.service.serviceName').toLowerCase() + '_hostname';
    } else if (this.get('parentView.service.serviceName') === 'KERBEROS') {
      return 'kdc_hosts';
    } else if (this.get('parentView.service.serviceName') === 'RANGER') {
      return '{0}_{1}_host'.format(this.get('parentView.service.serviceName').toLowerCase(), this.get('databaseName').toLowerCase());
    }
    return '{0}_existing_{1}_host'.format(this.get('parentView.service.serviceName').toLowerCase(), this.get('databaseName').toLowerCase());
  }.property('databaseName'),
  /** @property {boolean} isBtnDisabled - disable button on failed validation or active request **/
  isBtnDisabled: Em.computed.or('!isValidationPassed', 'isConnecting'),
  /** @property {object} requiredProperties - properties that necessary for database connection **/
  requiredProperties: function() {
    var ranger = App.StackService.find().findProperty('serviceName', 'RANGER');
    var propertiesMap = {
      OOZIE: ['oozie.db.schema.name', 'oozie.service.JPAService.jdbc.username', 'oozie.service.JPAService.jdbc.password', 'oozie.service.JPAService.jdbc.driver', 'oozie.service.JPAService.jdbc.url'],
      HIVE: ['ambari.hive.db.schema.name', 'javax.jdo.option.ConnectionUserName', 'javax.jdo.option.ConnectionPassword', 'javax.jdo.option.ConnectionDriverName', 'javax.jdo.option.ConnectionURL'],
      KERBEROS: ['kdc_hosts'],
      RANGER: ranger && App.StackService.find().findProperty('serviceName', 'RANGER').compareCurrentVersion('0.5') > -1 ?
          ['db_user', 'db_password', 'db_name', 'ranger.jpa.jdbc.url', 'ranger.jpa.jdbc.driver'] :
          ['db_user', 'db_password', 'db_name', 'ranger_jdbc_connection_url', 'ranger_jdbc_driver']
    };
    return propertiesMap[this.get('parentView.service.serviceName')];
  }.property(),
  /** @property {Object} propertiesPattern - check pattern according to type of connection properties **/
  propertiesPattern: function() {
    var patterns = {
      db_connection_url: /jdbc\.url|connection_url|connectionurl|kdc_hosts/ig
    };
    if (this.get('parentView.service.serviceName') != "KERBEROS") {
      patterns.user_name = /(username|dblogin|db_user)$/ig;
      patterns.user_passwd = /(dbpassword|password|db_password)$/ig;
    }
    return patterns;
  }.property('parentView.service.serviceName'),
  /** @property {String} masterHostName - host name location of Master Component related to Service **/
  masterHostName: function() {
    var serviceMasterMap = {
      'OOZIE': 'oozie_server_hosts',
      'HDFS': 'hadoop_host',
      'HIVE': 'hive_metastore_hosts',
      'KERBEROS': 'kdc_hosts',
      'RANGER': 'ranger_server_hosts'
    };
    return this.get('parentView.categoryConfigsAll').findProperty('name', serviceMasterMap[this.get('parentView.service.serviceName')]).get('value');
  }.property('parentView.service.serviceName', 'parentView.categoryConfigsAll.@each.value'),
  /** @property {Object} connectionProperties - service specific config values mapped for custom action request **/
  connectionProperties: function() {
    var propObj = {};
    for (var key in this.get('propertiesPattern')) {
      propObj[key] = this.getConnectionProperty(this.get('propertiesPattern')[key]);
    }

    if (this.get('parentView.service.serviceName') === 'RANGER') {
      var dbFlavor = this.get('parentView.categoryConfigsAll').findProperty('name','DB_FLAVOR').get('value'),
        /** TODO: Remove SQLA from the list of databases once Ranger DB_FLAVOR=SQLA is replaced with SQL Anywhere */
        databasesTypes = /MYSQL|POSTGRES|ORACLE|MSSQL|SQLA|Anywhere/gi,
        dbType = dbFlavor.match(databasesTypes)?dbFlavor.match(databasesTypes)[0].toLowerCase():'';

      if (dbType==='oracle') {
        // fixes oracle SYSDBA issue
        propObj['user_name'] = "\'%@ as sysdba\'".fmt(propObj['user_name']);
      }
    }
    return propObj;
  }.property('parentView.categoryConfigsAll.@each.value'),
  /**
   * Properties that stores in local storage used for handling
   * last success connection.
   *
   * @property {Object} preparedDBProperties
   **/
  preparedDBProperties: function() {
    var propObj = {};
    for (var key in this.get('propertiesPattern')) {
      var propName = this.getConnectionProperty(this.get('propertiesPattern')[key], true);
      propObj[propName] = this.get('parentView.categoryConfigsAll').findProperty('name', propName).get('value');
    }
    return propObj;
  }.property(),
  /** Check validation and load ambari properties **/
  didInsertElement: function() {
    var kdc = this.get('parentView.categoryConfigsAll').findProperty('name', 'kdc_type');
    if (kdc) {
      var name = kdc.get('value');
      if (name == 'Existing MIT KDC') {
        name = 'KDC';
      } else if (name == 'Existing IPA') {
        name = 'IPA';
      } else {
        name = 'AD';
      }
      App.popover(this.$('.connection-result'), {
        title: Em.I18n.t('services.service.config.database.btn.idle'),
        content: Em.I18n.t('installer.controls.checkConnection.popover').format(name),
        placement: 'right',
        trigger: 'hover'
      });
    }
    this.handlePropertiesValidation();
    this.getAmbariProperties();
  },
  /** On view destroy **/
  willDestroyElement: function() {
    this.set('isConnecting', false);
    this._super();
  },
  /**
   * Observer that take care about enabling/disabling button based on required properties validation.
   *
   * @method handlePropertiesValidation
   **/
  handlePropertiesValidation: function() {
    this.restore();
    var isValid = true;
    var properties = [].concat(this.get('requiredProperties'));
    properties.push(this.get('hostNameProperty'));
    properties.forEach(function(propertyName) {
      var property = this.get('parentView.categoryConfigsAll').findProperty('name', propertyName);
      if(property && !property.get('isValid')) isValid = false;
    }, this);
    this.set('isValidationPassed', isValid);
  }.observes('parentView.categoryConfigsAll.@each.isValid', 'parentView.categoryConfigsAll.@each.value', 'databaseName'),

  getConnectionProperty: function (regexp, isGetName) {
    var _this = this;
    var propertyName = _this.get('requiredProperties').filter(function (item) {
      return regexp.test(item);
    })[0];
    return (isGetName) ? propertyName : _this.get('parentView.categoryConfigsAll').findProperty('name', propertyName).get('value');
  },
  /**
   * Set up ambari properties required for custom action request
   *
   * @method getAmbariProperties
   **/
  getAmbariProperties: function() {
    var clusterController = App.router.get('clusterController');
    var _this = this;
    if (!App.isEmptyObject(App.db.get('tmp', 'ambariProperties')) && !this.get('ambariProperties')) {
      this.set('ambariProperties', App.db.get('tmp', 'ambariProperties'));
      return;
    }
    if (App.isEmptyObject(clusterController.get('ambariProperties'))) {
      clusterController.loadAmbariProperties().done(function(data) {
        _this.formatAmbariProperties(data.RootServiceComponents.properties);
      });
    } else {
      this.formatAmbariProperties(clusterController.get('ambariProperties'));
    }
  },

  formatAmbariProperties: function(properties) {
    var defaults = {
      threshold: "60",
      ambari_server_host: location.hostname,
      check_execute_list : "db_connection_check"
    };
    var properties = App.permit(properties, ['jdk.name','jdk_location','java.home']);
    var renameKey = function(oldKey, newKey) {
      if (properties[oldKey]) {
        defaults[newKey] = properties[oldKey];
        delete properties[oldKey];
      }
    };
    renameKey('java.home', 'java_home');
    renameKey('jdk.name', 'jdk_name');
    $.extend(properties, defaults);
    App.db.set('tmp', 'ambariProperties', properties);
    this.set('ambariProperties', properties);
  },
  /**
   * `Action` method for starting connect to current database.
   *
   * @method connectToDatabase
   **/
  connectToDatabase: function() {
    if (this.get('isBtnDisabled')) return;
    this.set('isRequestResolved', false);
    App.db.set('tmp', this.get('parentView.service.serviceName') + '_connection', {});
    this.setConnectingStatus(true);
    if (App.get('testMode')) {
      this.startPolling();
    } else {
      this.runCheckConnection();
    }
  },

  /**
   * runs check connections methods depending on service
   * @return {void}
   * @method runCheckConnection
   */
  runCheckConnection: function() {
    if (this.get('parentView.service.serviceName') === 'KERBEROS') {
      this.runKDCCheck();
    } else {
      this.createCustomAction();
    }
  },

  /**
   * send ajax request to perforn kdc host check
   * @return {App.ajax}
   * @method runKDCCheck
   */
  runKDCCheck: function() {
    return App.ajax.send({
      name: 'admin.kerberos_security.test_connection',
      sender: this,
      data: {
        kdcHostname: this.get('masterHostName')
      },
      success: 'onRunKDCCheckSuccess',
      error: 'onCreateActionError'
    });
  },

  /**
   *
   * @param data
   */
  onRunKDCCheckSuccess: function(data) {
    var statusCode = {
      success: 'REACHABLE',
      failed: 'UNREACHABLE'
    };
    if (data == statusCode['success']) {
      this.setResponseStatus('success');
    } else {
      this.setResponseStatus('failed');
    }
    this.set('responseFromServer', data);
  },

  /**
   * Run custom action for database connection.
   *
   * @method createCustomAction
   **/
  createCustomAction: function() {
    var isServiceInstalled = App.Service.find(this.get('parentView.service.serviceName')).get('isLoaded');
    var params = $.extend(true, {}, { db_name: this.get('databaseName').toLowerCase() }, this.get('connectionProperties'), this.get('ambariProperties'));
    App.ajax.send({
      name: (isServiceInstalled) ? 'cluster.custom_action.create' : 'custom_action.create',
      sender: this,
      data: {
        requestInfo: {
          parameters: params
        },
        filteredHosts: [this.get('masterHostName')]
      },
      success: 'onCreateActionSuccess',
      error: 'onCreateActionError'
    });
  },
  /**
   * Run updater if task is created successfully.
   *
   * @method onConnectActionS
   **/
  onCreateActionSuccess: function(data) {
    this.set('currentRequestId', data.Requests.id);
    App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('currentRequestId')
      },
      success: 'setCurrentTaskId'
    });
  },

  setCurrentTaskId: function(data) {
    this.set('currentTaskId', data.items[0].Tasks.id);
    this.startPolling();
  },

  startPolling: function() {
    if (this.get('isConnecting'))
      this.getTaskInfo();
  },

  getTaskInfo: function() {
    var request = App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('currentRequestId'),
        taskId: this.get('currentTaskId')
      },
      success: 'getTaskInfoSuccess'
    });
    this.set('request', request);
  },

  getTaskInfoSuccess: function(data) {
    var task = data.Tasks;
    this.set('responseFromServer', {
      stderr: task.stderr,
      stdout: task.stdout
    });
    if (task.status === 'COMPLETED') {
      var structuredOut = task.structured_out.db_connection_check;
      if (structuredOut.exit_code != 0) {
        this.set('responseFromServer', {
          stderr: task.stderr,
          stdout: task.stdout,
          structuredOut: structuredOut.message
        });
        this.setResponseStatus('failed');
      } else {
        App.db.set('tmp', this.get('parentView.service.serviceName') + '_connection', this.get('preparedDBProperties'));
        this.setResponseStatus('success');
      }
    }
    if (task.status === 'FAILED') {
      this.setResponseStatus('failed');
    }
    if (/PENDING|QUEUED|IN_PROGRESS/.test(task.status)) {
      Em.run.later(this, function() {
        this.startPolling();
      }, this.get('pollInterval'));
    }
  },

  onCreateActionError: function(jqXhr, status, errorMessage) {
    this.setResponseStatus('failed');
    this.set('responseFromServer', errorMessage);
  },

  setResponseStatus: function(isSuccess) {
    var isSuccess = isSuccess == 'success';
    this.setConnectingStatus(false);
    this.set('responseCaption', isSuccess ? Em.I18n.t('services.service.config.database.connection.success') : Em.I18n.t('services.service.config.database.connection.failed'));
    this.set('isConnectionSuccess', isSuccess);
    this.set('isRequestResolved', true);
    if (this.get('logsPopup')) {
      var statusString = isSuccess ? 'common.success' : 'common.error';
      this.set('logsPopup.header', Em.I18n.t('services.service.config.connection.logsPopup.header').format(this.get('databaseName'), Em.I18n.t(statusString)));
    }
  },
  /**
   * Switch captions and statuses for active/non-active request.
   *
   * @method setConnectionStatus
   * @param {Boolean} [active]
   */
  setConnectingStatus: function(active) {
    if (active) {
      this.set('responseCaption', Em.I18n.t('services.service.config.database.connection.inProgress'));
    }
    this.set('controller.testConnectionInProgress', !!active);
    this.set('btnCaption', !!active ? Em.I18n.t('services.service.config.database.btn.connecting') : Em.I18n.t('services.service.config.database.btn.idle'));
    this.set('isConnecting', !!active);
  },
  /**
   * Set view to init status.
   *
   * @method restore
   **/
  restore: function() {
    if (this.get('request')) {
      this.get('request').abort();
      this.set('request', null);
    }
    this.set('responseCaption', null);
    this.set('responseFromServer', null);
    this.setConnectingStatus(false);
    this.set('isRequestResolved', false);
  },
  /**
   * `Action` method for showing response from server in popup.
   *
   * @method showLogsPopup
   **/
  showLogsPopup: function() {
    if (this.get('isConnectionSuccess')) return;
    var _this = this;
    var statusString = this.get('isRequestResolved') ? 'common.error' : 'common.testing';
    var popup = App.showAlertPopup(Em.I18n.t('services.service.config.connection.logsPopup.header').format(this.get('databaseName'), Em.I18n.t(statusString)), null, function () {
      _this.set('logsPopup', null);
    });
    popup.reopen({
      onClose: function () {
        this._super();
        _this.set('logsPopup', null);
      }
    });
    if (typeof this.get('responseFromServer') == 'object') {
      popup.set('bodyClass', Em.View.extend({
        checkDBConnectionView: _this,
        templateName: require('templates/common/error_log_body'),
        openedTask: function () {
          return this.get('checkDBConnectionView.responseFromServer');
        }.property('checkDBConnectionView.responseFromServer.stderr', 'checkDBConnectionView.responseFromServer.stdout', 'checkDBConnectionView.responseFromServer.structuredOut')
      }));
    } else {
      popup.set('body', this.get('responseFromServer'));
    }
    this.set('logsPopup', popup);
    return popup;
  }
});

/**
 * View for switch group text
 *
 * @type {Em.View}
 */
App.SwitchToGroupView = Em.View.extend({

  group: null,

  tagName: 'a',

  classNames: ['action'],

  template: Ember.Handlebars.compile('{{ view.group.switchGroupTextShort }}'),

  didInsertElement: function() {
    var self = this;
    App.tooltip($(self.get('element')), {
      placement: 'top',
      title: self.get('group.switchGroupTextFull')
    });
  },

  willDestroyElement: function() {
    $(this.get('element')).tooltip('destroy');
  },

  click: function() {
    this.get('controller').selectConfigGroup({context: this.get('group')});
  }
});

/**
 * View with input field used to repo-version URLs
 * @type {*}
 */
App.BaseUrlTextField = Ember.TextField.extend({

  layout: Ember.Handlebars.compile('<div class="pull-left">{{yield}}</div> {{#if view.valueWasChanged}}<div class="pull-right"><a class="btn-sm" {{action "restoreValue" target="view"}}><i class="icon-undo"></i></a></div>{{/if}}'),

  /**
   * Binding in the template
   * @type {App.RepositoryVersion}
   */
  repository: null,

  /**
   * @type {string}
   */
  valueBinding: 'repository.baseUrl',

  /**
   * @type {string}
   */
  defaultValue: '',

  /**
   *  validate base URL
   */
  validate: function () {
    if (this.get('repository.skipValidation')) {
      this.set('repository.hasError', false);
    } else {
      this.set('repository.hasError', !(validator.isValidBaseUrl(this.get('value'))));
    }
    this.get('parentView').uiValidation();
  }.observes('value', 'repository.skipValidation'),

  /**
   * Determines if user have put some new value
   * @type {boolean}
   */
  valueWasChanged: function () {
    return this.get('value') !== this.get('defaultValue');
  }.property('value', 'defaultValue'),

  didInsertElement: function () {
    this.set('defaultValue', this.get('value'));
  },

  /**
   * Restore value and unset error-flag
   * @method restoreValue
   */
  restoreValue: function () {
    this.set('value', this.get('defaultValue'));
  }
});
