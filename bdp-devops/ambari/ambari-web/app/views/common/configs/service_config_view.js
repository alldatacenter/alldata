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

App.ServiceConfigView = Em.View.extend({

  templateName: require('templates/common/configs/service_config'),

  isRestartMessageCollapsed: false,

  isDiscardDisabled: Em.computed.or('!controller.versionLoaded', '!controller.isPropertiesChanged'),

  isSaveDisabled: Em.computed.or('controller.isSubmitDisabled', '!controller.versionLoaded', '!controller.isPropertiesChanged'),

  /**
   * Bound from parent view in the template
   * @type {string}
   */
  filter: '',

  /**
   * Determines that active tab is set during view initialize.
   * @type {boolean}
   */
  initialActiveTabIsSet: false,

  /**
   * Bound from parent view in the template
   * @type {object[]}
   */
  columns: [],

  propertyFilterPopover: [Em.I18n.t('services.service.config.propertyFilterPopover.title'), Em.I18n.t('services.service.config.propertyFilterPopover.content')],

  canEdit: true, // View is editable or read-only?

  supportsHostOverrides: function () {
    switch (this.get('controller.name')) {
      case 'wizardStep7Controller':
        return this.get('controller.selectedService.serviceName') !== 'MISC';
      case 'mainServiceInfoConfigsController':
      case 'mainHostServiceConfigsController':
        return true;
      default:
        return false;
    }
  }.property('controller.name', 'controller.selectedService'),

  showSavePanel: function() {
    return this.get('isOnTheServicePage') &&
           !this.get('controller.isCompareMode') &&
           this.get('controller.selectedVersionRecord.isCurrent') &&
           !this.get('controller.isHostsConfigsPage');
  }.property('isOnTheServicePage', 'controller.isCompareMode', 'controller.selectedVersionRecord.isCurrent', 'controller.isHostsConfigsPage'),

  /**
   * Determines if user is on the service configs page
   * @type {boolean}
   */
  isOnTheServicePage: Em.computed.equal('controller.name', 'mainServiceInfoConfigsController'),

  classNameBindings: ['isOnTheServicePage:serviceConfigs', 'controller.isCompareMode:settings-compare-layout'],

  /**
   * flag defines if any config match filter
   * true if all configs should be hidden
   * @type {boolean}
   */
  isAllConfigsHidden: false,

  /**
   * method that runs <code>updateFilterCounters<code> to
   * update filter counters for advanced tab
   * @method showHideAdvancedByFilter
   */
  showHideAdvancedByFilter: function () {
    Em.run.once(this, 'updateFilterCounters');
  }.observes('controller.selectedService.configs.@each.isHiddenByFilter'),

  /**
   * save configuration
   * @return {object}
   */
  save: function () {
    var controller = this.get('controller');
    var passwordWasChanged = this.get('controller.passwordConfigsAreChanged');
    return App.ModalPopup.show({
      header: Em.I18n.t('dashboard.configHistory.info-bar.save.popup.title'),
      serviceConfigNote: '',
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/save_configuration'),
        classNames: ['col-md-12'],
        showPasswordChangeWarning: passwordWasChanged,
        notesArea: Em.TextArea.extend({
          classNames: ['full-width'],
          value: passwordWasChanged ? Em.I18n.t('dashboard.configHistory.info-bar.save.popup.notesForPasswordChange') : '',
          placeholder: Em.I18n.t('dashboard.configHistory.info-bar.save.popup.placeholder'),
          didInsertElement: function () {
            if (this.get('value')) {
              this.onChangeValue();
            }
          },
          onChangeValue: function() {
            this.get('parentView.parentView').set('serviceConfigNote', this.get('value'));
          }.observes('value')
        })
      }),
      footerClass: Em.View.extend({
        templateName: require('templates/main/service/info/save_popup_footer')
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onSave: function () {
        const newVersionToBeCreated = Math.max.apply(null, App.ServiceConfigVersion.find().mapProperty('version')) + 1;
        controller.setProperties({
          saveConfigsFlag: true,
          serviceConfigVersionNote: this.get('serviceConfigNote'),
          preSelectedConfigVersion: Em.Object.create({
            version: newVersionToBeCreated,
            serviceName: controller.get('content.serviceName'),
            groupName: controller.get('selectedConfigGroup.name')
          })
        });
        controller.saveStepConfigs();
        this.hide();
      },
      onDiscard: function () {
        this.hide();
        controller.set('preSelectedConfigVersion', null);
        controller.loadStep();
      },
      onCancel: function () {
        this.hide();
      }
    });
  },

  /**
   * updates filter counters for advanced tab
   * @method updateFilterCounters
   */
  updateFilterCounters: function() {
    if (this.get('controller.selectedService.configs') && this.get('state') !== 'destroyed') {
      var categories = this.get('controller.selectedService.configCategories').mapProperty('name');
      var configsToShow = this.get('controller.selectedService.configs').filter(function(config) {
        return config.get('isHiddenByFilter') == false && categories.contains(config.get('category')) && config.get('isVisible');
      });
      var isAllConfigsHidden = configsToShow.get('length') == 0;
      var isAdvancedHidden = isAllConfigsHidden || configsToShow.filter(function (config) {
        return Em.isNone(config.get('isInDefaultTheme'));
      }).get('length') == 0;
      this.set('isAllConfigsHidden', isAllConfigsHidden);
      var advancedTab = App.Tab.find().filterProperty('serviceName', this.get('controller.selectedService.serviceName')).findProperty('isAdvanced');
      advancedTab && advancedTab.set('isAdvancedHidden', isAdvancedHidden);
    }
  },

  /**
   * Check for layout config supports.
   * @returns {Boolean}
   */
  supportsConfigLayout: function() {
    var supportedControllers = ['wizardStep7Controller', 'mainServiceInfoConfigsController', 'mainHostServiceConfigsController'];
     if (App.Tab.find().rejectProperty('isCategorized').someProperty('serviceName', this.get('controller.selectedService.serviceName')) && supportedControllers.contains(this.get('controller.name'))) {
      return !Em.isEmpty(App.Tab.find().rejectProperty('isCategorized').filterProperty('serviceName', this.get('controller.selectedService.serviceName')).filterProperty('isAdvanced', false));
    } else {
      return false;
    }
  }.property('controller.name', 'controller.selectedService'),

  toggleRestartMessageView: function () {
    this.$('.service-body').toggle('blind', 200);
    this.set('isRestartMessageCollapsed', !this.get('isRestartMessageCollapsed'));
  },

  didInsertElement: function () {
    if (this.get('isNotEditable') === true) {
      this.set('canEdit', false);
    }
    if (this.$('.service-body')) {
      this.$('.service-body').hide();
    }
    App.tooltip($(".restart-required-property"), {html: true});
    App.tooltip($(".glyphicon .glyphicon-lock"), {placement: 'right'});
    App.tooltip($("[rel=tooltip]"));
    this.checkCanEdit();
    this.set('filter', '');
  },

  willDestroyElement: function() {
    //Force configs remove in order to speed up rendering
    this.$().detach().remove();
    this._super();
  },

  /**
   * Check if we should show Custom Property category
   * @method checkCanEdit
   */
  checkCanEdit: function () {
    var controller = this.get('controller');
    if (!controller.get('selectedService.configCategories')) {
      return;
    }

    if (controller.get('selectedConfigGroup')) {
      controller.get('selectedService.configCategories').filterProperty('siteFileName').forEach(function (config) {
        var supportsAddingForbidden = App.config.shouldSupportAddingForbidden(controller.get('selectedService').serviceName, config.siteFileName); //true if the UI should not display the Custom ... section
        config.set('customCanAddProperty', !supportsAddingForbidden);
      });
    }

  }.observes('controller.selectedConfigGroup.name'),

  setActiveTab: function (event) {
    if (event.context.get('isHiddenByFilter')) return false;
    this.set('initialActiveTabIsSet', true);
    this.get('tabs').forEach(function (tab) {
      tab.set('isActive', false);
    });
    var currentTab = event.context;
    currentTab.set('isActive', true);
    currentTab.set('isRendered', true);
  },

  /**
   * Object that used for Twitter Bootstrap tabs markup.
   *
   * @returns {Ember.A}
   */
  tabs: function() {
    var tabs = App.Tab.find().rejectProperty('isCategorized').filterProperty('serviceName', this.get('controller.selectedService.serviceName'));
    var advancedTab = tabs.findProperty('isAdvanced', true);
    if (advancedTab) {
      advancedTab.set('isRendered', advancedTab.get('isActive'));
    }
    return tabs;
  }.property('controller.selectedService.serviceName'),

  /**
   * Set active tab when view attached and configs are linked to tabs.
   */
  initialActiveTabObserver: function() {
    var tabs = this.get('tabs').filterProperty('isAdvanced', false);
    if (tabs.everyProperty('isConfigsPrepared', true) && !this.get('initialActiveTabIsSet')) {
      this.pickActiveTab(this.get('tabs'));
      this.set('initialActiveTabIsSet', true);
    }
  }.observes('tabs.@each.isConfigsPrepared'),

  /**
   * Pick the first non hidden tab and make it active when there is no active tab
   * @method pickActiveTab
   */
  pickActiveTab: function (tabs) {
    if (!tabs) return;
    var activeTab = tabs.findProperty('isActive', true);
    if (activeTab) {
      if (activeTab.get('isHiddenByFilter')) {
        activeTab.set('isActive', false);
        this.pickActiveTab(tabs);
      }
    }
    else {
      var firstHotHiddenTab = tabs.filterProperty('isHiddenByFilter', false).get('firstObject');
      if(firstHotHiddenTab) {
        firstHotHiddenTab.set('isActive', true);
        if (firstHotHiddenTab.get('isAdvanced') && !firstHotHiddenTab.get('isRendered')) {
          firstHotHiddenTab.set('isRendered', true);
        }
      }
    }
  },

  /**
   * Mark isHiddenByFilter flag for configs, sub-sections, and tab
   * @method filterEnhancedConfigs
   */
  filterEnhancedConfigs: function () {
    if (!this.get('controller.selectedService') || this.get('state') === 'destroyed') return true;
    var self = this;

    var serviceConfigs = this.get('controller.selectedService.configs').filterProperty('isVisible', true);
    var filter = (this.get('filter')|| '').toLowerCase();
    var selectedFilters = (this.get('columns') || []).filterProperty('selected');

    if (selectedFilters.length > 0 || filter.length > 0) {
      serviceConfigs.forEach(function (config) {
        var passesFilters = true;

        selectedFilters.forEach(function (filter) {
          if (config.get(filter.attributeName) !== filter.attributeValue &&
            !(config.get('overrides') && config.get('overrides').someProperty(filter.attributeName, filter.attributeValue))) {
            passesFilters = false;
          }
        });

        if (!passesFilters) {
          config.set('isHiddenByFilter', true);
          return false;
        }

        var searchString = config.get('savedValue') + config.get('description') +
          config.get('displayName') + config.get('name') + config.get('value') + config.getWithDefault('stackConfigProperty.displayName', '');

        if (config.get('overrides')) {
          config.get('overrides').forEach(function (overriddenConf) {
            searchString += overriddenConf.get('value') + overriddenConf.get('group.name');
          });
        }

        if (filter != null && typeof searchString === "string") {
          config.set('isHiddenByFilter', !(searchString.toLowerCase().indexOf(filter) > -1));
        } else {
          config.set('isHiddenByFilter', false);
        }
      });
    }
    else {
      serviceConfigs.setEach('isHiddenByFilter', false);
    }

    Em.run.next(function () {
      self.pickActiveTab(self.get('tabs'));
    });
  }.observes('filter', 'columns.@each.selected', 'tabs.@each.isHiddenByFilter')
});
