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

App.ConfigVersionsControlView = Em.View.extend({
  templateName: require('templates/common/configs/config_versions_control'),

  serviceName: Em.computed.alias('controller.content.serviceName'),

  displayedServiceVersion: Em.computed.findBy('serviceVersions', 'isDisplayed', true),

  isCompareMode: Em.computed.notEqual('controller.compareServiceVersion', null),

  allServiceVersions: function() {
    return App.ServiceConfigVersion.find().filterProperty('serviceName', this.get('serviceName'));
  }.property('serviceName'),

  serviceVersions: function () {
    const isDefaultGroupSelected = this.get('controller.selectedConfigGroup.isDefault');
    const groupId = this.get('controller.selectedConfigGroup.id');

    const serviceVersions = this.get('allServiceVersions').filter(function(s) {
      return (s.get('groupId') === groupId || isDefaultGroupSelected && s.get('groupName') === App.ServiceConfigGroup.defaultGroupName);
    });

    if (!serviceVersions.findProperty('isDisplayed')) {
      //recompute serviceVersions if displayed version absent
      Em.run.next(() => this.propertyDidChange('controller.selectedConfigGroup.name'));
    }

    return serviceVersions.sort(function (a, b) {
      return Em.get(b, 'createTime') - Em.get(a, 'createTime');
    });
  }.property('serviceName', 'controller.selectedConfigGroup.name'),

  primaryServiceVersionsInCompare: function() {
    return this.get('serviceVersions').filter((sv) => sv.get('version') !== this.get('controller.compareServiceVersion.version'));
  }.property('serviceVersions', 'controller.compareServiceVersion'),

  secondaryServiceVersionsInCompare: function() {
    if (this.get('controller.compareServiceVersion')) {
      return this.get('serviceVersions')
        .filter((serviceVersion) => !serviceVersion.get('isDisplayed'))
        .map((serviceVersion) => {
        const copy = Em.Object.create({
          version: serviceVersion.get('version'),
          stackVersion: serviceVersion.get('stackVersion'),
          authorFormatted: serviceVersion.get('authorFormatted'),
          createdDate: serviceVersion.get('createdDate'),
          fullNotes: serviceVersion.get('fullNotes'),
          isCurrent: serviceVersion.get('isCurrent'),
        });
        copy.set('isDisplayed', serviceVersion.get('version') === this.get('controller.compareServiceVersion.version'));
        return copy;
      });
    } else {
      return [];
    }
  }.property('serviceVersions', 'controller.compareServiceVersion'),

  willInsertElement: function () {
    this.setDisplayVersion();
  },

  setDisplayVersion: function () {
    const serviceVersions = this.get('serviceVersions');
    const selectedVersion = this.get('controller.selectedVersion');
    serviceVersions.forEach(function (serviceVersion) {
      serviceVersion.set('isDisplayed', selectedVersion === serviceVersion.get('version'));
    });
  },

  onChangeConfigGroup: function () {
    const serviceVersions = this.get('serviceVersions');
    const selectedGroupName = this.get('controller.selectedConfigGroup.name');
    const preselectedVersion = this.get('controller.selectedVersion');

    serviceVersions.forEach(function (serviceVersion) {
      const isSelected = serviceVersion.get('version') === preselectedVersion && serviceVersion.get('groupName') === selectedGroupName;
      serviceVersion.set('isDisplayed', isSelected);
    });

    if (!serviceVersions.someProperty('isDisplayed')) {
      serviceVersions.forEach(function (serviceVersion) {
        if (serviceVersion.get('isCurrent') && serviceVersion.get('groupName') === selectedGroupName) {
          serviceVersion.set('isDisplayed', true);
        }
      });
    }
  }.observes('controller.selectedConfigGroup'),

  /**
   * switch configs view version to chosen
   */
  switchVersion: function (event, stayInCompare) {
    const version = event.contexts[0];
    if (this.get('serviceVersions').filterProperty('isDisplayed').someProperty('version', version)) {
      return;
    }

    this.get('serviceVersions').forEach(function (serviceVersion) {
      serviceVersion.set('isDisplayed', serviceVersion.get('version') === version);
    });
    this.get('controller').loadSelectedVersion(version, null, true);
  },

  switchPrimaryInCompare: function(event) {
    this.switchVersion({contexts: [event.contexts[0].get('version')]}, true);
  },

  /**
   * add config values of chosen version to view for comparison
   * add a second version-info-bar for the chosen version
   */
  compare: function (event) {
    const serviceConfigVersion = event.contexts[0];
    this.set('controller.compareServiceVersion', serviceConfigVersion);

    const controller = this.get('controller');
    controller.get('stepConfigs').clear();
    controller.loadCompareVersionConfigs(controller.get('allConfigs')).done(function() {
      controller.onLoadOverrides(controller.get('allConfigs'));
    });
    if ($(event.currentTarget).hasClass('compare-button')) {
      // after entering Compare mode compare button should be destroyed before "focusOut" event, otherwise JS error will be thrown
      event.view.destroy();
    }
  },

  removeCompareVersionBar: function () {
    const displayedVersion = this.get('displayedServiceVersion.version');

    this.get('serviceVersions').forEach(function (serviceVersion) {
      serviceVersion.set('isDisplayed', serviceVersion.get('version') === displayedVersion);
    });
    this.get('controller').loadSelectedVersion(displayedVersion);
  },

  /**
   * revert config values to chosen version and apply reverted configs to server
   */
  makeCurrent: function () {
    const self = this;
    const serviceConfigVersion = this.get('displayedServiceVersion');
    const versionText = serviceConfigVersion.get('versionText');
    return App.ModalPopup.show({
      header: Em.I18n.t('dashboard.configHistory.info-bar.makeCurrent.popup.title'),
      serviceConfigNote: Em.I18n.t('services.service.config.configHistory.makeCurrent.message').format(versionText),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/save_configuration'),
        classNames: ['col-md-12'],
        notesArea: Em.TextArea.extend({
          classNames: ['full-width'],
          value: Em.I18n.t('services.service.config.configHistory.makeCurrent.message').format(versionText),
          onChangeValue: function() {
            this.get('parentView.parentView').set('serviceConfigNote', this.get('value'));
          }.observes('value')
        })
      }),
      primary: Em.I18n.t('dashboard.configHistory.info-bar.revert.button'),
      secondary: Em.I18n.t('common.discard'),
      third: Em.I18n.t('common.cancel'),
      onPrimary: function () {
        serviceConfigVersion.set('serviceConfigNote', this.get('serviceConfigNote'));
        self.sendRevertCall(serviceConfigVersion);
        this.hide();
      },
      onSecondary: function () {
        // force <code>serviceVersions</code> recalculating
        self.propertyDidChange('controller.selectedConfigGroup.name');
        this._super();
      },
      onThird: function () {
        this.onSecondary();
      }
    });
  },

  /**
   * send PUT call to revert config to selected version
   * @param serviceConfigVersion
   */
  sendRevertCall: function (serviceConfigVersion) {
    App.ajax.send({
      name: 'service.serviceConfigVersion.revert',
      sender: this,
      data: {
        data: {
          "Clusters": {
            "desired_service_config_versions": {
              "service_config_version": serviceConfigVersion.get('version'),
              "service_name": serviceConfigVersion.get('serviceName'),
              "service_config_version_note": serviceConfigVersion.get('serviceConfigNote')
            }
          }
        }
      },
      success: 'sendRevertCallSuccess'
    });
  },

  sendRevertCallSuccess: function (data, opt, params) {
    // revert to an old version would generate a new version with latest version number,
    // so, need to loadStep to update
    this.get('controller').loadStep();
  }
});
