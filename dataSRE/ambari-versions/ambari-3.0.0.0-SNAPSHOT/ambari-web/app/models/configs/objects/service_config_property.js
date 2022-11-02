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

/**
 * @class ServiceConfigProperty
 */
App.ServiceConfigProperty = Em.Object.extend({

  name: '',
  displayName: '',

  /**
   * value that is shown on IU
   * and is changing by user
   * @type {String|null}
   */
  value: '',

  /**
   * value that is saved on cluster configs
   * and stored in /api/v1/clusters/{name}/configurations
   * @type {String|null}
   */
  savedValue: null,

  /**
   * value that is returned from server as recommended
   * or stored on stack
   * @type {String|null}
   */
  recommendedValue: null,

  /**
   * initial value of config. if value is saved it will be initial
   * otherwise first recommendedValue will be initial
   * @type {String|null}
   */
  initialValue: null,

  /**
   * value that is shown on IU
   * and is changing by user
   * @type {boolean}
   */
  isFinal: false,

  /**
   * value that is saved on cluster configs api
   * @type {boolean}
   */
  savedIsFinal: null,

  /**
   * value that is returned from server as recommended
   * or stored on stack
   * @type {boolean}
   */
  recommendedIsFinal: null,

  /**
   * @type {boolean}
   */
  supportsFinal: false,

  /**
   * Hint message to display in tooltip. Tooltip will be wrapped on question mark icon.
   * If value is <code>false</code> no tooltip and question mark icon.
   *
   * @type {boolean|string}
   */
  hintMessage: false,

  /**
   * Display label on the right side from input. In general used for checkbox only.
   *
   * @type {boolean}
   */
  rightSideLabel: false,

  /**
   * type of property
   * @type {String[]}
   * @default empty array
   */
  propertyType: [],

  /**
   * Text to be shown as placeholder
   * By default savedValue is shown as placeholder
   * @type {String}
   */
  placeholderText: '',

  /**
   * type of widget View
   * @type {string}
   * @default null
   */
  widgetType: null,

  /**
   * Placeholder used for configs with input type text
   */
  placeholder: function() {
    if (this.isEditable) {
      return this.get('placeholderText') || this.get('savedValue');
    }
    return null;
  }.property('isEditable', 'placeholderText', 'savedValue'),
  
  retypedPassword: '',
  description: '',
  displayType: 'string', // string, digits, number, directories, custom
  unit: '',
  category: 'General',
  isRequired: true, // by default a config property is required
  isReconfigurable: true, // by default a config property is reconfigurable
  isEditable: true, // by default a config property is editable
  disabledAsComponentAction: false, // is true for component action configs
  isNotEditable: Em.computed.not('isEditable'),
  hideFinalIcon: Em.computed.and('!isFinal', 'isNotEditable'),
  isVisible: true,
  isMock: false, // mock config created created only to displaying
  isRequiredByAgent: true, // Setting it to true implies property will be stored in configuration
  isSecureConfig: false,
  errorMessage: '',
  warnMessage: '',
  validationErrors: [], // stores messages from validation response marked as ERRROR
  validationWarnings: [], // stores message from validation response marked as WARN
  serviceConfig: null, // points to the parent App.ServiceConfig object
  filename: '',
  isOriginalSCP : true, // if true, then this is original SCP instance and its value is not overridden value.
  parentSCP: null, // This is the main SCP which is overridden by this. Set only when isOriginalSCP is false.
  overrides : null,
  overrideValues: [],
  group: null, // Contain group related to this property. Set only when isOriginalSCP is false.
  isUserProperty: null, // This property was added by user. Hence they get removal actions etc.
  isOverridable: true,
  compareConfigs: [],
  isComparison: false,
  hasCompareDiffs: false,
  showLabel: true,
  isConfigIdentity: false,
  copy: '',

  /**
   * Determines if config exists in the non-default config group but is loaded for default config group
   *
   * @type {boolean}
   */
  isCustomGroupConfig: false,

  /**
   * Determines if config is Undefined label, used for overrides, that do not have original property in default group
   * @type {boolean}
   */
  isUndefinedLabel: function () {
    return this.get('displayType') === 'label' && this.get('value') === 'Undefined';
  }.property('displayType', 'value'),

  error: Em.computed.bool('errorMessage.length'),
  warn: Em.computed.bool('warnMessage.length'),
  hasValidationErrors: Em.computed.bool('validationErrors.length'),
  hasValidationWarnings: Em.computed.bool('validationWarnings.length'),
  isValid: Em.computed.equal('errorMessage', ''),

  previousValue: null, // cached value before changing config <code>value</code>

  /**
   * List of <code>isFinal</code>-values for overrides
   * Set in the controller
   * Should be empty array by default!
   * @type {boolean[]}
   */
  overrideIsFinalValues: [],

  /**
   * true if property has warning or error
   * @type {boolean}
   */
  hasIssues: Em.computed.or('error', 'warn', 'overridesWithIssues.length'),

  overridesWithIssues: Em.computed.filterBy('overrides', 'hasIssues', true),

  index: null, //sequence number in category
  editDone: false, //Text field: on focusOut: true, on focusIn: false
  isNotSaved: false, // user property was added but not saved
  hasInitialValue: false, //if true then property value is defined and saved to server
  isHiddenByFilter: false, //if true then hide this property (filtered out)
  rowStyleClass: null, // CSS-Class to be applied on the row showing this config
  showAsTextBox: false,

  /**
   * config is invisible since wrapper section is hidden
   * @type {boolean}
   */
  hiddenBySection: false,

  /**
   * Determines config visibility on subsection level when wrapped.
   * @type {boolean}
   */
  hiddenBySubSection: false,

  /**
   * Determines visibility state including section/subsection state.
   * When <code>true</code> means that property is shown and may affect validation process.
   * When <code>false</code> means that property won't affect validation.
   */
  isActive: Em.computed.and('isVisible', '!hiddenBySubSection', '!hiddenBySection'),

  /**
   * @type {boolean}
   */
  recommendedValueExists: function () {
    return !Em.isNone(this.get('recommendedValue')) && (this.get('recommendedValue') != "")
      && this.get('isRequiredByAgent') && !this.get('cantBeUndone');
  }.property('recommendedValue'),

  /**
   * Usage example see on <code>App.ServiceConfigRadioButtons.handleDBConnectionProperty()</code>
   *
   * @property {Ember.View} additionalView - custom view related to property
   **/
  additionalView: null,

  /**
   * If config is saved we should compare config <code>value<code> with <code>savedValue<code> to
   * find out if it was changed, but if config in not saved there is no <code>savedValue<code>, so
   * we should use <code>initialValue<code> instead.
   */
  isNotInitialValue: function() {
    if (Em.isNone(this.get('savedValue')) && !Em.isNone(this.get('initialValue'))) {
      var value = this.get('value'), initialValue = this.get('initialValue');
      if (this.get('stackConfigProperty.valueAttributes.type') == 'float') {
        initialValue = !Em.isNone(initialValue) ? '' + parseFloat(initialValue) : null;
        value = '' + parseFloat(value);
      }
      return initialValue !== value;
    }
    return false;
  }.property('initialValue', 'savedValue', 'value', 'stackConfigProperty.valueAttributes.type'),

  /**
   * Is property has active override with error
   */
  isValidOverride: function () {
    return this.get('overrides.length') ? !this.get('overrides').find(function(o) {
     return Em.get(o, 'isEditable') && Em.get(o, 'errorMessage');
    }) : true;
  }.property("overrides.@each.errorMessage"),
  /**
   * No override capabilities for fields which are not edtiable
   * and fields which represent master hosts.
   */
  isPropertyOverridable: function () {
    var overrideable = this.get('isOverridable');
    var editable = this.get('isEditable');
    var overrides = this.get('overrides');
    var dt = this.get('displayType');
    return overrideable && (editable || !overrides || !overrides.length) && (!["componentHost", "password"].contains(dt));
  }.property('isEditable', 'displayType', 'isOverridable', 'overrides.length'),

  isOverridden: Em.computed.or('overrides.length', '!isOriginalSCP'),

  isOverrideChanged: function () {
    if (Em.isNone(this.get('overrides')) && this.get('overrideValues.length') === 0) return false;
    return JSON.stringify(this.get('overrides').mapProperty('isFinal')) !== JSON.stringify(this.get('overrideIsFinalValues'))
      || JSON.stringify(this.get('overrides').mapProperty('value')) !== JSON.stringify(this.get('overrideValues'));
  }.property('overrides.@each.isNotDefaultValue', 'overrides.@each.overrideIsFinalValues', 'overrideValues.length'),

  isRemovable: function() {
    return this.get('isEditable') && this.get('isRequiredByAgent') && !(this.get('overrides.length') > 0)
       && (this.get('isUserProperty') || !this.get('isOriginalSCP'));
  }.property('isUserProperty', 'isOriginalSCP', 'overrides.length', 'isRequiredByAgent'),

  init: function () {
    this.setInitialValues();
    this.set('viewClass', App.config.getViewClass(this.get("displayType"), this.get('dependentConfigPattern'), this.get('unit')));
    this.set('validateErrors', App.config.getErrorValidator(this.get("displayType")));
    this.set('validateWarnings', App.config.getWarningValidator(this.get("displayType")));
    this.validate();
  },

  setInitialValues: function () {
    if (Em.isNone(this.get('value'))) {
      if (!Em.isNone(this.get('savedValue'))) {
        this.set('value', this.get('savedValue'));
      } else if (!Em.isNone(this.get('recommendedValue'))) {
        this.set('value', this.get('recommendedValue'));
      }
    }
    this.set('previousValue', this.get('value'));
    if (this.get('value') === null) {
      this.set('isVisible', false);
    }
    if (this.get("displayType") === "password") {
      this.set('retypedPassword', this.get('value'));
      this.set('recommendedValue', '');
    }
    this.set('initialValue', this.get('value'));
  },

  /**
   * updates configs list that belongs to config group
   */
  updateGroupConfigs: function() {
    if (this.get('group')) {
      var o = this.get('group.properties').find(function(c) {
        return Em.get(c, 'name') === this.get('name') && Em.get(c, 'filename') === this.get('filename');
      }, this);

      if (o) {
        Em.set(o, 'value', this.get('value'));
      }
    }
  }.observes('value'),

  /**
   * Indicates when value is not the default value.
   * Returns false when there is no default value.
   *
   * @type {boolean}
   */
  isNotDefaultValue: function () {
    var value = this.get('value'),
      savedValue = this.get('savedValue'),
      supportsFinal = this.get('supportsFinal'),
      isFinal = this.get('isFinal'),
      savedIsFinal = this.get('savedIsFinal');

    // ignore precision difference for configs with type of `float` which value may ends with 0
    // e.g. between 0.4 and 0.40
    if (this.get('stackConfigProperty') && this.get('stackConfigProperty.valueAttributes.type') == 'float') {
      savedValue = !Em.isNone(savedValue) ? '' + parseFloat(savedValue) : null;
      value = '' + parseFloat(value);
    }
    return (savedValue != null && value !== savedValue) || (supportsFinal && !Em.isNone(savedIsFinal) && isFinal !== savedIsFinal);
  }.property('value', 'savedValue', 'isEditable', 'isFinal', 'savedIsFinal'),

  /**
   * Don't show "Undo" for hosts on Installer Step7
   */
  cantBeUndone: Em.computed.existsIn('displayType', ["componentHost", "componentHosts", "radio button"]),

  validate: function () {
    if ((typeof this.get('value') != 'object') && ((this.get('value') + '').length === 0)) {
      var widgetType = this.get('widgetType');
      this.set('errorMessage', (this.get('isRequired') && (!['test-db-connection','label'].contains(widgetType))) ? Em.I18n.t('errorMessage.config.required') : '');
    } else {
      this.set('errorMessage', this.validateErrors(this.get('value'), this.get('name'), this.get('retypedPassword')));
    }
    if (!this.get('widgetType') || ('text-field' === this.get('widgetType'))) {
      //temp conditions, since other warnings are calculated directly in widget view
      this.set('warnMessage', this.validateWarnings(this.get('value'), this.get('name'), this.get('filename'),
        this.get('stackConfigProperty'), this.get('unit')));
    }
  }.observes('value', 'retypedPassword', 'isEditable'),

  viewClass: App.ServiceConfigTextField,

  validateErrors: function() { return '' },

  validateWarnings: function() { return '' },

  /**
   * Get override for selected group
   *
   * @param {String} groupName
   * @returns {App.ServiceConfigProperty|null}
   */
  getOverride: function(groupName) {
    Em.assert('Group name should be defined string', (typeof groupName === 'string') && groupName);
    if (this.get('overrides.length')) {
      return this.get('overrides').findProperty('group.name', groupName);
    }
    return null;
  }

});
