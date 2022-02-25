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

App.MainHostDetailsView = Em.View.extend({
  templateName: require('templates/main/host/details'),

  /**
   * flag identify whether current host exist and loaded to model
   */
  isLoaded: false,

  content: Em.computed.alias('App.router.mainHostDetailsController.content'),

  clients: Em.computed.filterBy('content.hostComponents', 'isClient', true),

  clientsWithConfigs: function () {
    return this.get('content.hostComponents').filterProperty('isClient').filter(function (client) {
      return !App.get('services.noConfigTypes').contains(client.get('service.serviceName'));
    });
  }.property('content.hostComponents.@each'),

  hasManyClientsWithConfigs: Em.computed.gt('clientsWithConfigs.length', 1),

  maintenance: function () {
    var result = [];
    if (App.isAuthorized("SERVICE.START_STOP")) {
      result = result.concat([
        {
          action: 'startAllComponents',
          liClass: this.get('controller.content.isNotHeartBeating') ? 'disabled' : 'enabled',
          cssClass: 'glyphicon glyphicon-play',
          label: this.t('hosts.host.details.startAllComponents')
        },
        {
          action: 'stopAllComponents',
          liClass: this.get('controller.content.isNotHeartBeating') ? 'disabled' : 'enabled',
          cssClass: 'glyphicon glyphicon-stop',
          label: this.t('hosts.host.details.stopAllComponents')
        },
        {
          action: 'restartAllComponents',
          liClass: this.get('controller.content.isNotHeartBeating') ? 'disabled' : 'enabled',
          cssClass: 'glyphicon glyphicon-repeat',
          label: this.t('hosts.host.details.restartAllComponents')
        }
      ]);
    }
    if (App.isAuthorized("HOST.TOGGLE_MAINTENANCE")) {
      result.push({
        action: 'setRackId',
        liClass: '',
        cssClass: 'glyphicon glyphicon-cog',
        label: this.t('hosts.host.details.setRackId')
      });
      result.push({
        action: 'onOffPassiveModeForHost',
        liClass: '',
        cssClass: 'icon-medkit',
        active: this.get('controller.content.isActive'),
        label: this.t('passiveState.turn' + (this.get('controller.content.isActive') ? "On" : "Off"))
      });
    }
    if (App.get('isKerberosEnabled') && App.get('supports.regenerateKeytabsOnSingleHost')){
      var actionMap = App.HostComponentActionMap.getMap(this);
      result.push(actionMap.REGENERATE_KEYTAB_FILE_OPERATIONS);
    }    
    if (App.isAuthorized("HOST.ADD_DELETE_HOSTS")) {
      result.push({
        action: 'deleteHost',
        liClass: '',
        cssClass: 'glyphicon glyphicon-remove',
        label: this.t('hosts.host.details.deleteHost')
      });
    }
    result.push({
      action: 'checkHost',
      liClass: '',
      cssClass: 'glyphicon glyphicon-check',
      label: this.t('host.host.details.checkHost')
    });
    return result;
  }.property('controller.content', 'controller.content.isActive', 'controller.content.isNotHeartBeating'),

  didInsertElement: function () {
    var self = this;
    var host = self.get('content');

    this.set('isLoaded', App.Host.find(host.get('id')).get('isLoaded'));
    App.router.get('updateController').updateHost(function () {
      self.set('isLoaded', true);
      App.tooltip($("[rel='HealthTooltip']"));
      if (!host.get('isLoaded')) {
        //if host is not existed then route to list of hosts
        App.router.transitionTo('main.hosts.index');
      }
    });
  },

  willDestroyElement: function () {
    $("[rel='HealthTooltip']").tooltip('destroy');
  }

});
