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

App.MainSideMenuView = Em.CollectionView.extend({
  tagName: 'ul',
  classNames: ['nav', 'side-nav-menu', 'nav-pills', 'nav-stacked'],

  views: Em.computed.alias('App.router.mainViewsController.ambariViews'),

  didInsertElement: function() {
    $('[data-toggle="collapse-side-nav"]').on('click', () => {
      if ($('.navigation-bar-container.collapsed').length > 0) {
        App.tooltip($('.navigation-bar-container.collapsed .mainmenu-li:not(.has-sub-menu)>a'), {placement: "right"});
        App.tooltip($('.navigation-bar-container.collapsed .mainmenu-li.has-sub-menu>a'), {placement: "top"});
      } else {
        $('.navigation-bar-container .mainmenu-li>a').tooltip('destroy');
      }
    });
  },

  content: function () {
    var result = [];
    let {router} = App;
    if (router.get('loggedIn')) {

      if (router.get('clusterController.isLoaded') && App.get('router.clusterInstallCompleted')) {
        if (!App.get('isOnlyViewUser')) {
          result.push(
              {label: Em.I18n.t('menu.item.dashboard'), iconClass: 'glyphicon glyphicon-home', routing: 'dashboard', active: 'active', href: router.urlFor('main.dashboard')},
              {label: Em.I18n.t('menu.item.services'), iconClass: 'glyphicon glyphicon-briefcase', routing: 'services', href: router.urlFor('main.services')},
              {label: Em.I18n.t('menu.item.hosts'), iconClass: 'icon-tasks', routing: 'hosts', href: router.urlFor('main.hosts')},
              {label: Em.I18n.t('menu.item.alerts'), iconClass: 'glyphicon glyphicon-bell', routing: 'alerts', href: router.urlFor('main.alerts')}
          );
        }
        if (App.isAuthorized('CLUSTER.TOGGLE_KERBEROS, CLUSTER.MODIFY_CONFIGS, SERVICE.START_STOP, SERVICE.SET_SERVICE_USERS_GROUPS, CLUSTER.UPGRADE_DOWNGRADE_STACK, CLUSTER.VIEW_STACK_DETAILS')
            || (App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
          result.push({ label: Em.I18n.t('menu.item.admin'), iconClass: 'glyphicon glyphicon-wrench', routing: 'admin', href: router.urlFor('main.admin')});
        }
      }
    }
    return result;
  }.property(
      'App.router.loggedIn',
      'App.router.clusterController.isLoaded',
      'App.router.clusterInstallCompleted',
      'App.router.wizardWatcherController.isWizardRunning'
  ),

  itemViewClass: Em.View.extend({

    classNameBindings: ['dropdownMenu:dropdown'],

    classNames: ['mainmenu-li'],

    templateName: require('templates/main/side-menu-item'),

    dropdownMenu: Em.computed.existsIn('content.routing', ['services', 'admin']),
    isAdminItem: Em.computed.equal('content.routing', 'admin'),
    isServicesItem: Em.computed.equal('content.routing', 'services'),
    isViewsItem: function () {
      return this.get('content').routing.contains('views');
    }.property(''),
    goToSection: function (event) {
      if (event.context === 'hosts') {
        App.router.set('mainHostController.showFilterConditionsFirstLoad', false);
      } else if (event.context === 'views') {
        App.router.route('views');
        return;
      } else if (event.context === 'alerts') {
        App.router.set('mainAlertDefinitionsController.showFilterConditionsFirstLoad', false);
      }
      App.router.route('main/' + event.context);
    },

    selectedAdminItemBinding: 'App.router.mainAdminController.category',

    dropdownCategories: function () {
      var itemName = this.get('content.routing');
      var categories = [];
      var upg = App.get('upgradeInProgress') || App.get('upgradeHolding');
      // create dropdown categories for each menu item
      let {router} = App;
      if (itemName === 'admin') {
        if(App.isAuthorized('CLUSTER.VIEW_STACK_DETAILS, CLUSTER.UPGRADE_DOWNGRADE_STACK') || upg) {
          categories.push({
            name: 'stackAndUpgrade',
            url: 'stack',
            label: Em.I18n.t('admin.stackUpgrade.title'),
            href: router.urlFor('main.admin.stackAndUpgrade')
          });
        }
        if(App.isAuthorized('SERVICE.SET_SERVICE_USERS_GROUPS') || upg) {
          categories.push({
            name: 'adminServiceAccounts',
            url: 'serviceAccounts',
            label: Em.I18n.t('common.serviceAccounts'),
            disabled: App.get('upgradeInProgress') || App.get('upgradeHolding'),
            href: router.urlFor('main.admin.adminServiceAccounts')
          });
        }
        if (!App.get('isHadoopWindowsStack') && App.isAuthorized('CLUSTER.TOGGLE_KERBEROS') || upg) {
          if (App.supports.enableToggleKerberos) {
            categories.push({
              name: 'kerberos',
              url: 'kerberos/',
              label: Em.I18n.t('common.kerberos'),
              disabled: App.get('upgradeInProgress') || App.get('upgradeHolding'),
              href: router.urlFor('main.admin.adminKerberos')
            });
          }
        }
        if ((App.isAuthorized('SERVICE.START_STOP, CLUSTER.MODIFY_CONFIGS') && App.isAuthorized('SERVICE.MANAGE_AUTO_START, CLUSTER.MANAGE_AUTO_START')) || upg) {
          if (App.supports.serviceAutoStart) {
            categories.push({
              name: 'serviceAutoStart',
              url: 'serviceAutoStart',
              label: Em.I18n.t('admin.serviceAutoStart.title'),
              href: router.urlFor('main.admin.adminServiceAutoStart')
            });
          }
        }
      }
      return categories;
    }.property('content.routing'),

    AdminDropdownItemView: Ember.View.extend({
      tagName: 'li',
      classNameBindings: ['isActive:active', 'isDisabled:disabled'],
      classNames: ['submenu-li'],
      isActive: Em.computed.equalProperties('item', 'parentView.selectedAdminItem'),
      isDisabled: function () {
        return !!this.get('parentView.dropdownCategories').findProperty('name', this.get('item')).disabled;
      }.property('item', 'parentView.dropdownCategories.@each.disabled'),
      goToCategory: function (event) {
        var itemName = this.get('parentView').get('content').routing;
        // route to correct category of current menu item
        // skip routing to already selected category
        if (itemName === 'admin' && !this.get('isActive') && !this.get('isDisabled')) {
          App.router.route('main/admin/' + event.context);
        }
      }
    })
  })
});

App.SideNavServiceMenuView = Em.CollectionView.extend({
  disabledServices: [],

  content: function () {
    return App.router.get('mainServiceController.content').filter(function (item) {
      return !this.get('disabledServices').contains(item.get('id'));
    }, this);
  }.property('App.router.mainServiceController.content.length').volatile(),

  didInsertElement:function () {
    App.router.location.addObserver('lastSetURL', this, 'renderOnRoute');
    this.renderOnRoute();
    App.tooltip(this.$(".restart-required-service"), {html:true, placement:"right"});
    App.tooltip($("[rel='serviceHealthTooltip']"), {html:true, placement:"right"});
    App.tooltip(this.$(".passive-state-service"), {html: true, placement: "top"});
  },

  willDestroyElement: function() {
    App.router.location.removeObserver('lastSetURL', this, 'renderOnRoute');
    this.$(".restart-required-service").tooltip('destroy');
    this.$(".passive-state-service").tooltip('destroy');
  },

  activeServiceId:null,
  /**
   *    Syncs navigation menu with requested URL
   */
  renderOnRoute:function () {
    var lastUrl = App.router.location.lastSetURL || location.href.replace(/^[^#]*#/, '');
    if (lastUrl.substr(1, 4) !== 'main' || !this._childViews) {
      return;
    }
    var reg = /^\/main\/services\/(\S+)\//g;
    var subUrl = reg.exec(lastUrl);
    var serviceId = (null != subUrl) ? subUrl[1] : 1;
    this.set('activeServiceId', serviceId);
  },

  tagName:'ul',
  classNames:[ 'sub-menu', 'nav', 'nav-pills', 'nav-stacked', 'services-submenu'],

  itemViewClass:Em.View.extend({

    classNameBindings:["clients"],
    classNames: ["submenu-li"],
    templateName:require('templates/main/service/menu_item'),
    restartRequiredMessage: null,

    shouldBeRestarted: Em.computed.someBy('content.hostComponents', 'staleConfigs', true),

    isMasterDown: function() {
      return this.get('content.hostComponents').filterProperty('isMaster').some((component) => {
        return !component.get('isRunning');
      });
    }.property('content.hostComponents.@each.workStatus'),

    isClientOnlyService : function(){
      return App.get('services.clientOnly').contains(this.get('content.serviceName'));
    }.property('content.serviceName'),
    
    displayName: function() {
      if (this.get('content.hasMasterOrSlaveComponent') || this.get('content.displayName').endsWith('Client')) {
        return this.get('content.displayName');
      } else {
        return this.get('content.displayName') + ' Client';
      }
    }.property('content.displayName', 'content.hasMasterOrSlaveComponent'),

    isConfigurable: function () {
      return !App.get('services.noConfigTypes').contains(this.get('content.serviceName'));
    }.property('App.services.noConfigTypes','content.serviceName'),

    /**
     * '#/main/services/SERVICE_ID'
     *
     * @type {string}
     */
    dataHref: function () {
      return App.router.urlFor('main.services.service', {service_id: this.get('content.id')});
    }.property('content.id'),

    link: function() {
      var stateName = (['summary','configs'].contains(App.router.get('currentState.name')))
          ? this.get('isConfigurable') ? App.router.get('currentState.name') : 'summary'
          : 'summary';
      return "#/main/services/" + this.get('content.id') + "/" + stateName;
    }.property('App.router.currentState.name', 'parentView.activeServiceId','isConfigurable'),

    goToConfigs: function () {
      App.router.set('mainServiceItemController.routeToConfigs', true);
      App.router.transitionTo('services.service.configs', this.get('content'));
      App.router.set('mainServiceItemController.routeToConfigs', false);
    },

    refreshRestartRequiredMessage: function() {
      var restarted, componentsCount, hostsCount, message, tHosts, tComponents;
      restarted = this.get('content.restartRequiredHostsAndComponents');
      componentsCount = 0;
      hostsCount = 0;
      message = "";
      for (var host in restarted) {
        hostsCount++;
        componentsCount += restarted[host].length;
      }
      if (hostsCount > 1) {
        tHosts = Em.I18n.t('common.hosts');
      } else {
        tHosts = Em.I18n.t('common.host');
      }
      if (componentsCount > 1) {
        tComponents = Em.I18n.t('common.components');
      } else {
        tComponents = Em.I18n.t('common.component');
      }
      message += componentsCount + ' ' + tComponents + ' ' + Em.I18n.t('on') + ' ' +
          hostsCount + ' ' + tHosts + ' ' + Em.I18n.t('services.service.config.restartService.needToRestartEnd');
      this.set('restartRequiredMessage', message);
    }.observes('content.restartRequiredHostsAndComponents')
  })

});
