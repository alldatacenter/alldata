/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

require('views/common/table_view');

var App = require('app');
var validator = require('utils/validator');

module.exports = {

  /**
   * Launches a dialog to select hosts from the provided available hosts.
   *
   * Once the user clicks OK or Cancel, the callback is called with the
   * array of hosts (App.Host[]) selected. If the dialog was cancelled
   * or closed, <code>null</code> is provided to the callback. Else
   * an array (maybe empty) will be provided to the callback.
   *
   * @param initialHosts  {App.Host[]} List of hosts to pick from
   * @param selectedHosts {App.Host[]} List of hosts already selected from the available hosts
   * @param selectAtleastOneHost  {boolean} If true atleast one host has to be selected
   * @param validComponents {App.HostComponent[]} List of host-component types to pick from.
   * @param callback  Callback function which is invoked when dialog
   * @param popupDescription {Object} Consist header and message for popup
   *   Example: {header: 'header', dialogMessage: 'message'}
   *  is closed, cancelled or OK is pressed.
   */
  launchHostsSelectionDialog : function(initialHosts, selectedHosts,
      selectAtleastOneHost, validComponents, callback, popupDescription) {
    // set default popup description
    var defaultPopupDescription = {
      header: Em.I18n.t('hosts.selectHostsDialog.title'),
      dialogMessage: Em.I18n.t('hosts.selectHostsDialog.message')
    };
    if (popupDescription !== null) {
      popupDescription = $.extend(true, defaultPopupDescription, popupDescription);
    }
    App.ModalPopup.show({

      classNames: ['common-modal-wrapper', 'full-height-modal'],
      modalDialogClasses: ['modal-lg'],

      elementId: 'host-selection-dialog',

      header: popupDescription.header,

      dialogMessage: popupDescription.dialogMessage,

      warningMessage: null,

      availableHosts: [],

      onPrimary: function () {
        this.set('warningMessage', null);
        var arrayOfSelectedHosts = this.get('availableHosts').filterProperty('selected', true).mapProperty('host.id');
        if (selectAtleastOneHost && arrayOfSelectedHosts.length < 1) {
          this.set('warningMessage', Em.I18n.t('hosts.selectHostsDialog.message.warning'));
          return;
        }
        callback(arrayOfSelectedHosts);
        this.hide();
      },

      disablePrimary: Em.computed.not('isLoaded'),

      onSecondary: function () {
        callback(null);
        this.hide();
      },

      bodyClass: App.TableView.extend({

        templateName: require('templates/common/configs/overrideWindow'),

        controllerBinding: 'App.router.mainServiceInfoConfigsController',

        isPaginate: true,

        filteredContent: [],

        filteredContentObs: function() {
          Em.run.once(this, this.filteredContentObsOnce);
        }.observes('parentView.availableHosts.@each.filtered'),

        filteredContentObsOnce: function() {
          var filteredContent = this.get('parentView.availableHosts').filterProperty('filtered') || [];
          this.set('filteredContent', filteredContent);
        },

        filterText: '',

        filterTextPlaceholder: Em.I18n.t('hosts.selectHostsDialog.filter.placeHolder'),

        filterColumn: null,

        filterColumns: Ember.A([
          Ember.Object.create({id: 'ip', name: 'IP Address', selected: true}),
          Ember.Object.create({id: 'cpu', name: 'CPU', selected: false}),
          Ember.Object.create({id: 'memory', name: 'RAM', selected: false}),
          Ember.Object.create({id: 'osArch', name: 'OS Architecture', selected: false}),
          Ember.Object.create({id: 'osType', name: 'OS Type', selected: false}),
          Ember.Object.create({id: 'diskTotal', name: 'Total Disks Capacity', selected: false}),
          Ember.Object.create({id: 'disksMounted', name: '# of Disk Mounts', selected: false})
        ]),

        showOnlySelectedHosts: false,

        filterComponents: validComponents,

        filterComponent: null,

        isDisabled: Em.computed.not('parentView.isLoaded'),

        didInsertElement: function() {
          var defaultFilterColumn = this.get('filterColumns').findProperty('selected');
          this.get('filterComponents').setEach('selected', false);
          this.set('filterColumn', defaultFilterColumn);
          initialHosts.setEach('filtered', true);
          this.set('parentView.availableHosts', initialHosts);
          this.set('parentView.isLoaded', true);
          this.filteredContentObsOnce();
        },

        /**
         * Default filter-method isn't needed
         */
        filter: Em.K,

        filterHosts: function () {
          var filterText = this.get('filterText');
          var showOnlySelectedHosts = this.get('showOnlySelectedHosts');
          var filterComponent = this.get('filterComponent');
          var filterColumn = this.get('filterColumn');

          this.get('parentView.availableHosts').forEach(function (host) {
            var skip = showOnlySelectedHosts && !host.get('selected');
            var value = host.get('host').get(filterColumn.id);
            var hostComponentNames = host.get('hostComponentNames');

            host.set('filterColumnValue', value);

            if (!skip && filterText && (value == null || !value.toString().match(filterText)) && !host.get('host.publicHostName').match(filterText)) {
              skip = true;
            }
            if (!skip && filterComponent && hostComponentNames.length > 0) {
              skip = !hostComponentNames.contains(filterComponent.get('componentName'));
            }
            host.set('filtered', !skip);
          }, this);

          this.set('startIndex', this.get('parentView.availableHosts').someProperty('filtered') ? 1 : 0);
        }.observes('parentView.availableHosts', 'filterColumn', 'filterText', 'filterComponent', 'filterComponent.componentName', 'showOnlySelectedHosts'),

        hostSelectMessage: function () {
          var hosts = this.get('parentView.availableHosts');
          var selectedHosts = hosts.filterProperty('selected', true);
          return this.t('hosts.selectHostsDialog.selectedHostsLink').format(selectedHosts.get('length'), hosts.get('length'))
        }.property('parentView.availableHosts.@each.selected'),

        selectFilterColumn: function (event) {
          if (event != null && event.context != null && event.context.id != null) {
            var filterColumn = this.get('filterColumn');
            if (filterColumn != null) {
              filterColumn.set('selected', false);
            }
            event.context.set('selected', true);
            this.set('filterColumn', event.context);
          }
        },

        selectFilterComponent: function (event) {
          if (event != null && event.context != null && event.context.componentName != null) {
            var currentFilter = this.get('filterComponent');
            if (currentFilter != null) {
              currentFilter.set('selected', false);
            }
            if (currentFilter != null && currentFilter.componentName === event.context.componentName) {
              // selecting the same filter deselects it.
              this.set('filterComponent', null);
            } else {
              this.set('filterComponent', event.context);
              event.context.set('selected', true);
            }
          }
        },

        allHostsSelected: false,

        toggleSelectAllHosts: function (event) {
          this.get('parentView.availableHosts').filterProperty('filtered').setEach('selected', this.get('allHostsSelected'));
        }.observes('allHostsSelected'),

        toggleShowSelectedHosts: function () {
          var currentFilter = this.get('filterComponent');
          if (currentFilter != null) {
            currentFilter.set('selected', false);
          }
          this.setProperties({
            filterComponent: null,
            filterText: null
          });
          this.toggleProperty('showOnlySelectedHosts');
        }
      })
    });
  },

   /**
   * Bulk setting of for rack id
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Ember.Enumerable} hosts - list of affected hosts
   */
  setRackInfo: function (operationData, hosts, rackId) {
    var self = this;
    var hostNames = hosts.mapProperty('hostName');
    return App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.details.setRackId'),
      disablePrimary: true,
      rackId: rackId ? rackId : "",
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/rack_id_popup'),
        errorMessage: null,
        isValid: true,
        validation: function () {
          this.set('isValid', validator.isValidRackId(this.get('parentView.rackId')));
          this.set('errorMessage', this.get('isValid') ? '' : Em.I18n.t('hostPopup.setRackId.invalid'));
          this.set('parentView.disablePrimary', !this.get('isValid'));
        }.observes('parentView.rackId')
      }),
      onPrimary: function() {
        var rackId = this.get('rackId');
        if (hostNames.length) {
          App.ajax.send({
            name: 'bulk_request.hosts.update_rack_id',
            sender: self,
            data: {
              hostNames: hostNames.join(','),
              requestInfo: operationData.message,
              rackId: rackId,
              hostNamesArray: hostNames
            },
            success: 'successRackId',
            error: 'errorRackId'
          });
        }
        this.hide();
      }
    });
  },

  /**
   * Success callback for set rack id request
   */
  successRackId: function (response, request, params) {
    App.Host.find().forEach(function(host){
      if (params.hostNamesArray.contains(host.get('hostName'))) {
        host.set('rack', params.rackId)
      }
    });
  },

  /**
   * Warn user that the rack id will not be updated
   */
  errorRackId: function () {
    App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('hostPopup.setRackId.error'));
  }
};
