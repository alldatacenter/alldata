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

var App = require('app');
var batchUtils = require('utils/batch_scheduled_requests');
var hostsManagement = require('utils/hosts');
var O = Em.Object;

/**
 * @class BulkOperationsController
 */
App.BulkOperationsController = Em.Controller.extend({

  name: 'bulkOperationsController',

  /**
   * Bulk operation wrapper
   * @param {Object} operationData - data about bulk operation (action, hosts or hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   * @method bulkOperation
   */
  bulkOperation: function (operationData, hosts) {
    if (operationData.componentNameFormatted) {
      if (operationData.action === 'RESTART') {
        this.bulkOperationForHostComponentsRestart(operationData, hosts);
      }
      else if (operationData.action === 'ADD') {
        this.bulkOperationForHostComponentsAdd(operationData, hosts);
      }
      else if (operationData.action === 'DELETE') {
        this.bulkOperationForHostComponentsDelete(operationData, hosts);
      }
      else {
        if (operationData.action.indexOf('DECOMMISSION') == -1) {
          this.bulkOperationForHostComponents(operationData, hosts);
        }
        else {
          this.bulkOperationForHostComponentsDecommission(operationData, hosts);
        }
      }
    }
    else {
      if (operationData.action === 'SET_RACK_INFO') {
        this.bulkOperationForHostsSetRackInfo(operationData, hosts);
      }
      else {
        if (operationData.action === 'RESTART') {
          this.bulkOperationForHostsRestart(operationData, hosts);
        }
        else if (operationData.action === 'REINSTALL'){
          this.bulkOperationForHostsReinstall(operationData, hosts);
        }
        else if (operationData.action === 'DELETE'){
          this._bulkOperationForHostsDelete(hosts);
        }
        else if (operationData.action === 'CONFIGURE') {
            this.bulkOperationForHostsRefreshConfig(operationData, hosts);
        }
        else {
          if (operationData.action === 'PASSIVE_STATE') {
            this.bulkOperationForHostsPassiveState(operationData, hosts);
          }
          else {
            this.bulkOperationForHosts(operationData, hosts);
          }
        }
      }
    }
  },

  /**
   * Bulk operation (start/stop all) for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   * @return {$.ajax}
   */
  bulkOperationForHosts: function (operationData, hosts) {
    var self = this;
    return batchUtils.getComponentsFromServer({
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF',
      displayParams: ['host_components/HostRoles/component_name']
    }, function (data) {
      return self._getComponentsFromServerForHostsCallback(operationData, data);
    });
  },

  /**
   * run Bulk operation (start/stop all) for selected hosts
   * after host and components are loaded
   * @param operationData
   * @param data
   */
  _getComponentsFromServerForHostsCallback: function (operationData, data) {
    var query = [];
    var hostNames = [];
    var hostsMap = {};
    var clients = App.components.get('clients');

    data.items.forEach(function (host) {
      host.host_components.forEach(function (hostComponent) {
        if (!clients.contains((hostComponent.HostRoles.component_name))) {
          if (hostsMap[host.Hosts.host_name]) {
            hostsMap[host.Hosts.host_name].push(hostComponent.HostRoles.component_name);
          } else {
            hostsMap[host.Hosts.host_name] = [hostComponent.HostRoles.component_name];
          }
        }
      });
    });

    var nn_hosts = [];
    for (var hostName in hostsMap) {
      if (hostsMap.hasOwnProperty(hostName)) {
        var subQuery = '(HostRoles/component_name.in(%@)&HostRoles/host_name=' + hostName + ')';
        var components = hostsMap[hostName];

        if (components.length) {
          if (components.contains('NAMENODE')) {
            nn_hosts.push(hostName);
          }
          query.push(subQuery.fmt(components.join(',')));
        }
        hostNames.push(hostName);
      }
    }
    hostNames = hostNames.join(",");
    if (query.length) {
      query = query.join('|');
      var self = this;
      // if NameNode included, check HDFS NameNode checkpoint before stop NN
      var isHDFSStarted = 'STARTED' === App.Service.find('HDFS').get('workStatus');

      var request = function () {
        return App.ajax.send({
          name: 'common.host_components.update',
          sender: self,
          data: {
            query: query,
            HostRoles: {
              state: operationData.action
            },
            context: operationData.message,
            hostName: hostNames,
            noOpsMessage: Em.I18n.t('hosts.host.maintainance.allComponents.context')
          },
          success: 'bulkOperationForHostComponentsSuccessCallback'
        });
      };

      if (operationData.action === 'INSTALLED' && isHDFSStarted) {
        if (nn_hosts.length == 1) {
          return App.router.get('mainHostDetailsController').checkNnLastCheckpointTime(request, nn_hosts[0]);
        }
        if (nn_hosts.length > 1) {
          // HA or federation enabled
          return App.router.get('mainServiceItemController').checkNnLastCheckpointTime(request);
        }
      }
      return request();
    }
    else {
      return App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(Em.I18n.t('hosts.host.maintainance.allComponents.context')),
        secondary: false
      });
    }
  },

  bulkOperationForHostsSetRackInfo: function (operationData, hosts) {
    return hostsManagement.setRackInfo(operationData, hosts);
  },

  /**
   * Bulk restart for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Ember.Enumerable} hosts - list of affected hosts
   */
  bulkOperationForHostsRestart: function (operationData, hosts) {
    return batchUtils.getComponentsFromServer({
      passiveState: 'OFF',
      hosts: hosts.mapProperty('hostName'),
      displayParams: ['host_components/HostRoles/component_name']
    }, this._getComponentsFromServerForRestartCallback);
  },

  /**
   *
   * @param {object} data
   * @private
   * @method _getComponentsFromServerCallback
   */
  _getComponentsFromServerForRestartCallback: function (data) {
    var hostComponents = [];
    data.items.forEach(function (host) {
      host.host_components.forEach(function (hostComponent) {
        hostComponents.push(O.create({
          componentName: hostComponent.HostRoles.component_name,
          hostName: host.Hosts.host_name
        }));
      })
    });
    // if NameNode included, check HDFS NameNode checkpoint before restart NN
    var isHDFSStarted = 'STARTED' === App.Service.find('HDFS').get('workStatus');
    var namenodes = hostComponents.filterProperty('componentName', 'NAMENODE');
    var nn_count = namenodes.get('length');

    if (nn_count == 1 && isHDFSStarted) {
      var hostName = namenodes.get('firstObject.hostName');
      App.router.get('mainHostDetailsController').checkNnLastCheckpointTime(function () {
        batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.allOnSelectedHosts'), "HOST");
      }, hostName);
    }
    else {
      if (nn_count > 1 && isHDFSStarted) {
        // HA or federation enabled
        App.router.get('mainServiceItemController').checkNnLastCheckpointTime(function () {
          batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.allOnSelectedHosts'), "HOST");
        });
      }
      else {
        batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.allOnSelectedHosts'), "HOST");
      }
    }
  },

  /**
   * Bulk reinstall failed components for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Ember.Enumerable} hosts - list of affected hosts
   */
  bulkOperationForHostsReinstall: function (operationData, hosts) {
    var self = this;
    App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
      return App.ajax.send({
        name: 'common.host_components.update',
        sender: self,
        data: {
          HostRoles: {
            state: 'INSTALLED'
          },
          query: 'HostRoles/host_name.in(' + hosts.mapProperty('hostName').join(',') + ')&HostRoles/state=INSTALL_FAILED',
          context: operationData.message,
          noOpsMessage: Em.I18n.t('hosts.host.maintainance.reinstallFailedComponents.context')
        },
        success: 'bulkOperationForHostComponentsSuccessCallback',
        showLoadingPopup: true
      });
    });
  },

  /**
  * Check which hosts can be deleted and warn the user about it in advance
  * @param {Ember.Enumerable} hosts - list of affected hosts
  */
  _bulkOperationForHostsDelete: function (hosts) {
    var self = this,
        hostNamesToDelete = [],
        hostsNotToDelete = [];
    var createNonDeletableComponents = function (hostName, message) {
      return Em.Object.create({
        error: {
          key: hostName,
          message: message
        },
        isCollapsed: true,
        isBodyVisible: Em.computed.ifThenElse('isCollapsed', 'display: none;', 'display: block;')
      });
    };
    hosts.forEach(function (host) {
      var hostComponents = App.HostComponent.find().filterProperty('hostName', host.hostName);
      var hostInfo = App.router.get('mainHostDetailsController').getHostComponentsInfo(hostComponents);
      console.dir(hostInfo);
      if (hostInfo.nonDeletableComponents.length > 0) {
        hostsNotToDelete.push(createNonDeletableComponents(host.hostName, Em.I18n.t('hosts.bulkOperation.deleteHosts.nonDeletableComponents').format(hostInfo.nonDeletableComponents.join(", "))));
      } else if (hostInfo.nonAddableMasterComponents.length > 0) {
        hostsNotToDelete.push(createNonDeletableComponents(host.hostName, Em.I18n.t('hosts.bulkOperation.deleteHosts.nonAddableMasterComponents').format(hostInfo.nonAddableMasterComponents.join(", "))));
      } else if (hostInfo.lastMasterComponents.length > 0) {
        hostsNotToDelete.push(createNonDeletableComponents(host.hostName, Em.I18n.t('hosts.bulkOperation.deleteHosts.lastMasterComponents').format(hostInfo.lastMasterComponents.join(", "))));
      } else if (hostInfo.runningComponents.length > 0) {
        hostsNotToDelete.push(createNonDeletableComponents(host.hostName, Em.I18n.t('hosts.bulkOperation.deleteHosts.runningComponents').format(hostInfo.runningComponents.join(", "))));
      } else {
        hostNamesToDelete.push(host.hostName);
      }
    });

    return App.ModalPopup.show({
      header: hostNamesToDelete.length ? Em.I18n.t('hosts.bulkOperation.deleteHosts.confirm.header') : Em.I18n.t('rolling.nothingToDo.header'),
      primary: hostNamesToDelete.length ? Em.I18n.t('common.next') : null,
      primaryClass: 'btn-default',
      onPrimary: function () {
        this._super();
        self.bulkOperationForHostsDelete(hostNamesToDelete);
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/bulk_add_delete_confirm_popup'),
        modifyMessage: Em.I18n.t('hosts.bulkOperation.deleteHosts.confirm.delete'),
        skipMessage: hostNamesToDelete.length ? Em.I18n.t('hosts.bulkOperation.deleteHosts.cannot.delete1') : Em.I18n.t('hosts.bulkOperation.deleteHosts.cannot.delete2'),
        skippedHosts: hostsNotToDelete.length ? hostsNotToDelete : null,
        hostsToModify: hostNamesToDelete.length ? hostNamesToDelete.join("\n") : null,
        onToggleHost: function (host) {
          host.contexts[0].toggleProperty('isCollapsed');
        }
      })
    });
  },

  /**
   * Bulk refresh configs for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Ember.Enumerable} hosts - list of affected/selected hosts
   */
  bulkOperationForHostsRefreshConfig: function (operationData, hosts) {
    return batchUtils.getComponentsFromServer({
      passiveState: 'OFF',
      hosts: hosts.mapProperty('hostName'),
      displayParams: ['host_components/HostRoles/component_name']
    }, this._getComponentsFromServerForRefreshConfigsCallback);
  },

  /**
   *
   * @param {object} data
   * @private
   * @method _getComponentsFromServerForRefreshConfigsCallback
   */
  _getComponentsFromServerForRefreshConfigsCallback: function (data) {
    var hostComponents = [];
    var clients = App.components.get('clients');
    data.items.forEach(function (host) {
      host.host_components.forEach(function (hostComponent) {
        if (clients.contains((hostComponent.HostRoles.component_name))) {
          hostComponents.push(O.create({
            componentName: hostComponent.HostRoles.component_name,
            hostName: host.Hosts.host_name
          }));
        }
      })
    });
    batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.configs.allOnSelectedHosts'), "HOST");
  },

  /**
   * Bulk delete selected hosts
   * @param {String} hosts - list of affected host names
   */
  bulkOperationForHostsDelete: function (hosts) {
    var confirmKey = 'delete',
        self = this;
    App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
      return App.ModalPopup.show({
        header: Em.I18n.t('hosts.bulkOperation.deleteHosts.confirmation.header'),
        confirmInput: '',
        disablePrimary: Em.computed.notEqual('confirmInput', confirmKey),
        primary: Em.I18n.t('common.confirm'),
        primaryClass: 'btn-warning',
        onPrimary: function () {
          this._super();
          return App.ajax.send({
            name: 'common.hosts.delete',
            sender: self,
            data: {
              query: 'Hosts/host_name.in(' + hosts.join(',') + ')',
              hosts: hosts
            },
            success: 'bulkOperationForHostsDeleteCallback',
            error: 'bulkOperationForHostsDeleteCallback',
            showLoadingPopup: true
          });
        },
        bodyClass: Em.View.extend({
          templateName: require('templates/main/host/delete_hosts_popup'),
          hostNames: hosts,
          typeMessage: Em.I18n.t('services.service.confirmDelete.popup.body.type').format(confirmKey),
        })
      });
    });
  },

  /**
   * Show popup after bulk delete hosts
   * @method bulkOperationForHostsDeleteCallback
   */
  bulkOperationForHostsDeleteCallback: function (arg0, arg1, arg2, arg3, arg4) {
    var deletedHosts = [];
    var undeletableHosts = [];
    if (arg1 == "error") {
      var request = arg0;
      var params = arg4;
      var response = JSON.parse(request.responseText);
      var host = Ember.Object.create({
        error: {
          key: params.hosts[0],
          code: response.status,
          message: response.message
        },
        isCollapsed: true,
        isBodyVisible: Em.computed.ifThenElse('isCollapsed', 'display: none;', 'display: block;')
      });
      undeletableHosts.push(host);
    } else {
      var data = arg0;
      var params = arg2;
      if (data) {
        data.deleteResult.forEach(function (host) {
          if (!host.deleted) {
            var _host = Ember.Object.create({
              error: host.error,
              isCollapsed: true,
              isBodyVisible: Em.computed.ifThenElse('isCollapsed', 'display: none;', 'display: block;')
            });
            undeletableHosts.push(_host);
          }
        });
      }
      deletedHosts = params.hosts;
    }

    return App.ModalPopup.show({
      header: Em.I18n.t('hosts.bulkOperation.deleteHosts.result.header'),

      secondary: null,

      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/delete_hosts_result_popup'),
        message: Em.I18n.t('hosts.bulkOperation.deleteHosts.dryRun.message').format(undeletableHosts.length),
        undeletableHosts: undeletableHosts,
        deletedHosts: deletedHosts,
        onToggleHost: function (host) {
          host.contexts[0].toggleProperty('isCollapsed');
        }
      }),

      completeDelete() {
        if (arg1 !== 'error') {
          App.db.unselectHosts(arg2.hosts);
        }
        location.reload();
      },

      onPrimary: function () {
        this.completeDelete();
        this._super();
      },

      onClose: function () {
        this.completeDelete();
        this._super();
      }
    });
  },

  /**
   * Bulk turn on/off passive state for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostsPassiveState: function (operationData, hosts) {
    var self = this;

    return batchUtils.getComponentsFromServer({
      hosts: hosts.mapProperty('hostName'),
      displayParams: ['Hosts/maintenance_state']
    }, function (data) {
      return self._getComponentsFromServerForPassiveStateCallback(operationData, data)
    });
  },

  /**
   *
   * @param {object} operationData
   * @param {object} data
   * @returns {$.ajax|App.ModalPopup}
   * @private
   * @method _getComponentsFromServerForPassiveStateCallback
   */
  _getComponentsFromServerForPassiveStateCallback: function (operationData, data) {
    var hostNames = [];

    data.items.forEach(function (host) {
      if (host.Hosts.maintenance_state !== operationData.state) {
        hostNames.push(host.Hosts.host_name);
      }
    });
    if (hostNames.length) {
      return App.ajax.send({
        name: 'bulk_request.hosts.passive_state',
        sender: this,
        data: {
          hostNames: hostNames.join(','),
          passive_state: operationData.state,
          requestInfo: operationData.message
        },
        success: 'updateHostPassiveState'
      });
    }
    return App.ModalPopup.show({
      header: Em.I18n.t('rolling.nothingToDo.header'),
      body: Em.I18n.t('hosts.bulkOperation.passiveState.nothingToDo.body'),
      secondary: false
    });
  },

  updateHostPassiveState: function (data, opt, params) {
    return batchUtils.infoPassiveState(params.passive_state);
  },

  /**
   * bulk add for selected hostComponent
   * @param {Object} operationData - data about bulk operation (action, hostComponent etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostComponentsAdd: function (operationData, hosts) {
    var self = this;
    return batchUtils.getComponentsFromServer({
      components: [operationData.componentName],
      hosts: hosts.mapProperty('hostName')
    }, function (data) {
      return self._getComponentsFromServerForHostComponentsAddCallback(operationData, data, hosts);
    });
  },

  _getComponentsFromServerForHostComponentsAddCallback: function (operationData, data, hosts) {
    var self = this;

    var allHostsWithComponent = data.items.mapProperty('Hosts.host_name');
    var hostsWithComponent = [];
    hosts.forEach(function (host) {
      const isNotHeartBeating = host.state === 'HEARTBEAT_LOST';
      if(allHostsWithComponent.contains(host.hostName) || isNotHeartBeating) {
        hostsWithComponent.push(Em.Object.create({
          error: {
            key: host.hostName,
            message: isNotHeartBeating ? Em.I18n.t('hosts.bulkOperation.confirmation.add.component.noHeartBeat.skip') : Em.I18n.t('hosts.bulkOperation.confirmation.add.component.skip').format(operationData.componentNameFormatted)
          },
          isCollapsed: true,
          isBodyVisible: Em.computed.ifThenElse('isCollapsed', 'display: none;', 'display: block;')
        }));
      }
    });
    var hostsWithOutComponent = hosts.filter(function(host) {
      return !hostsWithComponent.findProperty('error.key', host.hostName);
    });

    hostsWithOutComponent = hostsWithOutComponent.mapProperty('hostName');

    return App.ModalPopup.show({
      header: hostsWithOutComponent.length ? Em.I18n.t('hosts.bulkOperation.confirmation.header') : Em.I18n.t('rolling.nothingToDo.header'),
      primary: hostsWithOutComponent.length ? Em.I18n.t('hosts.host.addComponent.popup.confirm') : null,

      onPrimary: function() {
        self.bulkAddHostComponents(operationData, hostsWithOutComponent);
        this._super();
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/bulk_add_delete_confirm_popup'),
        modifyMessage: Em.I18n.t('hosts.bulkOperation.confirmation.add.component').format(operationData.componentNameFormatted),
        skipMessage: hostsWithOutComponent.length ? Em.I18n.t('hosts.bulkOperation.confirmation.cannot.add1') : Em.I18n.t('hosts.bulkOperation.confirmation.cannot.add2').format(operationData.componentNameFormatted),
        hostsToModify: hostsWithOutComponent.length ? hostsWithOutComponent.join("\n") : null,
        skippedHosts: hostsWithComponent.length ? hostsWithComponent : null,
        onToggleHost: function (host) {
          host.contexts[0].toggleProperty('isCollapsed');
        }
      })
    });
  },
  /**
   * Bulk add for selected hostComponent
   * @param {Object} operationData - data about bulk operation (action, hostComponent etc)
   * @param {Array} hostNames - list of affected hosts' names
   */
  bulkAddHostComponents: function (operationData, hostNames) {
    var self= this;
    App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
      App.ajax.send({
        name: 'host.host_component.add_new_components',
        sender: self,
        data: {
          data: JSON.stringify({
            RequestInfo: {
              query: 'Hosts/host_name.in(' + hostNames.join(',') + ')'
            },
            Body: {
              host_components: [
                {
                  HostRoles: {
                    component_name: operationData.componentName
                  }
                }
              ]
            }
          }),
          context: operationData.message + ' ' + operationData.componentNameFormatted,
        },
        success: 'bulkOperationForHostComponentsAddSuccessCallback',
        showLoadingPopup: true
      });
    });
  },

  bulkOperationForHostComponentsAddSuccessCallback: function (data, opt, params) {
    App.ajax.send({
      name: 'common.host_components.update',
      sender: this,
      data: {
        query: 'HostRoles/state=INIT',
        HostRoles: {
          state: 'INSTALLED'
        },
        context: params.context
      },
      success: 'bulkOperationForHostComponentsSuccessCallback'
    });
  },

  /**
   * Confirm bulk delete for selected hostComponent
   * @param {Object} operationData - data about bulk operation (action, hostComponent etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostComponentsDelete: function (operationData, hosts) {
    var self = this;
    return batchUtils.getComponentsFromServer({
      components: [operationData.componentName],
      hosts: hosts.mapProperty('hostName'),
      displayParams: ['host_components/HostRoles/state']
    }, function (data) {
      return self._getComponentsFromServerForHostComponentsDeleteCallback(operationData, data, hosts);
    });
  },

  _getComponentsFromServerForHostComponentsDeleteCallback: function (operationData, data, requestedHosts) {
    var self = this;
    var minToInstall = App.StackServiceComponent.find(operationData.componentName).get('minToInstall');
    var installedCount = App.HostComponent.getCount(operationData.componentName, 'totalCount');
    var installedHosts = data.items.mapProperty('Hosts.host_name');
    var hostsToDelete = data.items.filter(function (host) {
      var state = host.host_components[0].HostRoles.state;
      return [App.HostComponentStatus.stopped, App.HostComponentStatus.unknown, App.HostComponentStatus.install_failed, App.HostComponentStatus.upgrade_failed, App.HostComponentStatus.init].contains(state);
    }).mapProperty('Hosts.host_name');

    if (installedCount - hostsToDelete.length < minToInstall) {
      return App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('hosts.bulkOperation.confirmation.delete.component.minimum.body').format(minToInstall, operationData.componentNameFormatted),
        secondary: false
      });
    }

    var hostsNotToDelete = [];

    requestedHosts.mapProperty('hostName').forEach(function (host) {
      if (!hostsToDelete.contains(host)) {
        var hostToSkip = Em.Object.create({
          error : {
            key: host,
            message: null,
          },
          isCollapsed : true,
          isBodyVisible: Em.computed.ifThenElse('isCollapsed', 'display: none;', 'display: block;')
        });
        if(installedHosts.contains(host)) {
          hostToSkip.error.message = Em.I18n.t('hosts.bulkOperation.confirmation.delete.component.notStopped').format(operationData.componentNameFormatted);
        } else {
          hostToSkip.error.message = Em.I18n.t('hosts.bulkOperation.confirmation.delete.component.notInstalled').format(operationData.componentNameFormatted);
        }
        hostsNotToDelete.push(hostToSkip);
      }
    });

    return App.ModalPopup.show({
      header: hostsToDelete.length ? Em.I18n.t('hosts.bulkOperation.confirmation.header') : Em.I18n.t('rolling.nothingToDo.header'),
      primary: hostsToDelete.length ? Em.I18n.t('hosts.host.deleteComponent.popup.confirm') : null,
      primaryClass: 'btn-warning',

      onPrimary: function() {
        self.bulkDeleteHostComponents(operationData, hostsToDelete);
        this._super();
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/bulk_add_delete_confirm_popup'),
        modifyMessage: Em.I18n.t('hosts.bulkOperation.confirmation.delete.component').format(operationData.componentNameFormatted),
        skipMessage: hostsToDelete.length ? Em.I18n.t('hosts.bulkOperation.confirmation.delete.component.cannot1') :  Em.I18n.t('hosts.bulkOperation.confirmation.delete.component.cannot2').format(operationData.componentNameFormatted),
        hostsToModify: hostsToDelete.length ? hostsToDelete.join("\n") : null,
        skippedHosts: hostsNotToDelete.length ? hostsNotToDelete : null,
        onToggleHost: function (host) {
          host.contexts[0].toggleProperty('isCollapsed');
        }
      })
    });
  },

  /**
   * Bulk delete for selected hostComponent
   * @param {Object} operationData - data about bulk operation (action, hostComponent etc)
   * @param {Array} hostNames - list of affected hosts' names
   */
  bulkDeleteHostComponents: function (operationData, hostNames) {
    var self= this;
    App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
      App.ajax.send({
        name: 'host.host_component.delete_components',
        sender: self,
        data: {
          hostNames,
          componentName: operationData.componentName,
          data: JSON.stringify({
            RequestInfo: {
              query: 'HostRoles/host_name.in(' + hostNames.join(',') + ')&HostRoles/component_name.in(' + operationData.componentName + ')'
            }
          })
        },
        success: 'bulkOperationForHostComponentsDeleteCallback'
      });
    });
  },

  /**
   * Show popup after bulk delete host_components
   * @method bulkOperationForHostComponentsDeleteCallback
   */
  bulkOperationForHostComponentsDeleteCallback: function (arg0, arg1, arg2, arg3, arg4) {
    var deletedHosts = [];
    var undeletableHosts = [];
    var componentName = arg2.componentName;
    if (arg1 == "error") {
      var request = arg0;
      let params = arg4;
      var response = JSON.parse(request.responseText);
      var host = Ember.Object.create({
        error: {
          key: params.hosts[0],
          code: response.status,
          message: response.message
        },
        isCollapsed: true,
        isBodyVisible: Em.computed.ifThenElse('isCollapsed', 'display: none;', 'display: block;')
      });
      undeletableHosts.push(host);
    } else {
      var data = arg0;
      let params = arg2;
      if (data) {
        data.deleteResult.forEach(function (host) {
          if (!host.deleted) {
            var _host = Ember.Object.create({
              error: host.error,
              isCollapsed: true,
              isBodyVisible: Em.computed.ifThenElse('isCollapsed', 'display: none;', 'display: block;')
            });
            undeletableHosts.push(_host);
          }
        });
      }
      deletedHosts = (params.hostNames);
    }

    return App.ModalPopup.show({
      header: Em.I18n.t('hosts.bulkOperation.delete.component.result.header'),

      secondary: null,

      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/delete_hosts_result_popup'),
        message: Em.I18n.t('hosts.bulkOperation.delete.component.dryRun.message').format(componentName),
        componentName: componentName,
        undeletableHosts: undeletableHosts,
        deletedHosts: deletedHosts,
        deleteComponents: true,
        onToggleHost: function (host) {
          host.contexts[0].toggleProperty('isCollapsed');
        }
      }),

      onPrimary: function () {
        location.reload();
        this._super();
      },

      onClose: function () {
        location.reload();
        this._super();
      }
    });
  },

  /**
   * Bulk operation for selected hostComponents
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostComponents: function (operationData, hosts) {
    var self = this;

    return batchUtils.getComponentsFromServer({
      components: [operationData.componentName],
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF'
    }, function (data) {
      return self._getComponentsFromServerForHostComponentsCallback(operationData, data)
    });
  },

  /**
   *
   * @param {object} operationData
   * @param {object} data
   * @returns {$.ajax|App.ModalPopup}
   * @private
   */
  _getComponentsFromServerForHostComponentsCallback: function (operationData, data) {
    if (data.items) {
      var hostsWithComponentInProperState = data.items.mapProperty('Hosts.host_name');
      return App.ajax.send({
        name: 'common.host_components.update',
        sender: this,
        data: {
          HostRoles: {
            state: operationData.action
          },
          query: 'HostRoles/component_name=' + operationData.componentName + '&HostRoles/host_name.in(' + hostsWithComponentInProperState.join(',') + ')&HostRoles/maintenance_state=OFF',
          context: operationData.message + ' ' + operationData.componentNameFormatted,
          level: 'SERVICE',
          noOpsMessage: operationData.componentNameFormatted
        },
        success: 'bulkOperationForHostComponentsSuccessCallback'
      });
    }
    return App.ModalPopup.show({
      header: Em.I18n.t('rolling.nothingToDo.header'),
      body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
      secondary: false
    });
  },

  /**
   * Bulk decommission/recommission for selected hostComponents
   * @param {Object} operationData
   * @param {Array} hosts
   */
  bulkOperationForHostComponentsDecommission: function (operationData, hosts) {
    var self = this;

    return batchUtils.getComponentsFromServer({
      components: [operationData.realComponentName],
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF',
      displayParams: ['host_components/HostRoles/state']
    }, function (data) {
      return self._getComponentsFromServerForHostComponentsDecommissionCallBack(operationData, data)
    });
  },

  /**
   * run Bulk decommission/recommission for selected hostComponents
   * after host and components are loaded
   * @param operationData
   * @param data
   * @method _getComponentsFromServerForHostComponentsDecommissionCallBack
   */
  _getComponentsFromServerForHostComponentsDecommissionCallBack: function (operationData, data) {
    var service = App.Service.find(operationData.serviceName);
    var components = [];

    data.items.forEach(function (host) {
      host.host_components.forEach(function (hostComponent) {
        components.push(O.create({
          componentName: hostComponent.HostRoles.component_name,
          hostName: host.Hosts.host_name,
          workStatus: hostComponent.HostRoles.state
        }))
      });
    });

    if (components.length) {
      var hostsWithComponentInProperState = components.mapProperty('hostName');
      var turn_off = operationData.action.indexOf('OFF') !== -1;
      var svcName = operationData.serviceName;
      var masterName = operationData.componentName;
      var slaveName = operationData.realComponentName;
      var hostNames = hostsWithComponentInProperState.join(',');
      if (turn_off) {
        // For recommession
        if (svcName === "YARN" || svcName === "HBASE" || svcName === "HDFS") {
          App.router.get('mainHostDetailsController').doRecommissionAndStart(hostNames, svcName, masterName, slaveName);
        }
      } else {
        hostsWithComponentInProperState = components.filterProperty('workStatus', 'STARTED').mapProperty('hostName');
        //For decommession
        if (svcName == "HBASE") {
          // HBASE service, decommission RegionServer in batch requests
          this.warnBeforeDecommission(hostNames);
        } else {
          var parameters = {
            "slave_type": slaveName
          };
          var contextString = turn_off ? 'hosts.host.' + slaveName.toLowerCase() + '.recommission' :
          'hosts.host.' + slaveName.toLowerCase() + '.decommission';
          if (turn_off) {
            parameters['included_hosts'] = hostsWithComponentInProperState.join(',')
          }
          else {
            parameters['excluded_hosts'] = hostsWithComponentInProperState.join(',');
          }
          App.ajax.send({
            name: 'bulk_request.decommission',
            sender: this,
            data: {
              context: Em.I18n.t(contextString),
              serviceName: service.get('serviceName'),
              componentName: operationData.componentName,
              parameters: parameters,
              noOpsMessage: operationData.componentNameFormatted
            },
            success: 'bulkOperationForHostComponentsSuccessCallback'
          });
        }
      }
    }
    else {
      App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
        secondary: false
      });
    }
  },


  /**
   * get info about regionserver passive_state
   * @method warnBeforeDecommission
   * @param {String} hostNames
   * @return {$.ajax}
   */
  warnBeforeDecommission: function (hostNames) {
    return App.ajax.send({
      'name': 'host_components.hbase_regionserver.active',
      'sender': this,
      'data': {
        hostNames: hostNames
      },
      success: 'warnBeforeDecommissionSuccess'
    });
  },

  /**
   * check is hbase regionserver in mm. If so - run decommission
   * otherwise shows warning
   * @method warnBeforeDecommission
   * @param {Object} data
   * @param {Object} opt
   * @param {Object} params
   */
  warnBeforeDecommissionSuccess: function(data, opt, params) {
    if (Em.get(data, 'items.length')) {
      return App.router.get('mainHostDetailsController').showHbaseActiveWarning();
    }
    return App.router.get('mainHostDetailsController').checkRegionServerState(params.hostNames);
  },

  /**
   * Bulk restart for selected hostComponents
   * @param {Object} operationData
   * @param {Array} hosts
   */
  bulkOperationForHostComponentsRestart: function (operationData, hosts) {
    var self = this;
    return batchUtils.getComponentsFromServer({
      components: [operationData.componentName],
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF',
      displayParams: ['Hosts/maintenance_state', 'host_components/HostRoles/stale_configs', 'host_components/HostRoles/maintenance_state']
    }, function (data) {
      return self._getComponentsFromServerForHostComponentsRestartCallback(operationData, data);
    });
  },

  _getComponentsFromServerForHostComponentsRestartCallback: function (operationData, data) {
    var wrappedHostComponents = [];
    var service = App.Service.find(operationData.serviceName);

    data.items.forEach(function (host) {
      host.host_components.forEach(function (hostComponent) {
        wrappedHostComponents.push(O.create({
          componentName: hostComponent.HostRoles.component_name,
          serviceName: operationData.serviceName,
          hostName: host.Hosts.host_name,
          hostPassiveState: host.Hosts.maintenance_state,
          staleConfigs: hostComponent.HostRoles.stale_configs,
          passiveState: hostComponent.HostRoles.maintenance_state
        }));
      });
    });

    if (wrappedHostComponents.length) {
      return batchUtils.showRollingRestartPopup(wrappedHostComponents.objectAt(0).get('componentName'), service.get('displayName'), service.get('passiveState') === "ON", false, wrappedHostComponents);
    }
    return App.ModalPopup.show({
      header: Em.I18n.t('rolling.nothingToDo.header'),
      body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
      secondary: false
    });
  },

  updateHostComponentsPassiveState: function (data, opt, params) {
    return batchUtils.infoPassiveState(params.passive_state);
  },

  /**
   * Show BO popup after bulk request
   * @method bulkOperationForHostComponentsSuccessCallback
   */
  bulkOperationForHostComponentsSuccessCallback: function (data, opt, params, req) {
    if (!data && req.status == 200) {
      return App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(params.noOpsMessage || Em.I18n.t('hosts.host.maintainance.allComponents.context')),
        secondary: false
      });
    }
    return App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  /**
   * Returns all hostNames if amount is less than {minShown} or
   * first elements of array (number of elements - {minShown}) converted to string
   * @param {Array} hostNames - array of all listed hostNames
   * @param {String} divider - string to separate hostNames
   * @param {Number} minShown - min amount of hostName to be shown
   * @returns {String} hostNames
   * @method _showHostNames
   * @private
   */
  _showHostNames: function(hostNames, divider, minShown) {
    if (hostNames.length > minShown) {
      return hostNames.slice(0, minShown).join(divider) + divider + Em.I18n.t("installer.step8.other").format(hostNames.length - minShown);
    }
    return hostNames.join(divider);
  },

  /**
   * Confirmation Popup for bulk Operations
   */
  bulkOperationConfirm: function(operationData, selection) {
    var hostsNames = [],
      queryParams = [];
    // @todo remove using external controller
    switch(selection) {
      case 's':
        hostsNames = App.db.getSelectedHosts('mainHostController');
        if(hostsNames.length > 0){
          queryParams.push({
            key: 'Hosts/host_name',
            value: hostsNames,
            type: 'MULTIPLE'
          });
        }
        break;
      case 'f':
        queryParams = App.router.get('mainHostController').getQueryParameters(true).filter(function (obj) {
          return !(obj.key == 'page_size' || obj.key == 'from');
        });
        break;
    }

    if (operationData.action === 'SET_RACK_INFO') {
      this.getHostsForBulkOperations(queryParams, operationData, null);
      return;
    }

    this.getHostsForBulkOperations(queryParams, operationData);
  },

  getHostsForBulkOperations: function (queryParams, operationData) {
    return App.ajax.send({
      name: 'hosts.bulk.operations',
      sender: this,
      data: {
        parameters: App.router.get('updateController').computeParameters(queryParams),
        operationData: operationData
      },
      success: 'getHostsForBulkOperationSuccessCallback',
      showLoadingPopup: true
    });
  },

  _convertHostsObjects: function (hosts) {
    return hosts.map(function (host) {
      return {
        index: host.index,
        id: host.id,
        clusterId: host.cluster_id,
        passiveState: host.passive_state,
        state: host.state,
        hostName: host.host_name,
        hostComponents: host.host_components
      }
    });
  },

  getHostsForBulkOperationSuccessCallback: function(json, opt, param) {
    var self = this;
    var operationData = param.operationData;
    var hosts = this._convertHostsObjects(App.hostsMapper.map(json, true));
    var repoVersion = null;
    // no hosts - no actions
    if (!hosts.length) {
      return;
    }

    if (['SET_RACK_INFO', 'ADD', 'DELETE'].contains(operationData.action)) {
      return self.bulkOperation(operationData, hosts);
    }

     var hostNames = hosts.mapProperty('hostName');
     var hostNamesSkipped = [];
     if ('DECOMMISSION' === operationData.action) {
       hostNamesSkipped = this._getSkippedForDecommissionHosts(json, hosts, operationData);
     }
     if ('PASSIVE_STATE' === operationData.action) {
       repoVersion = App.StackVersion.find().findProperty('isCurrent');
       if (!repoVersion && App.StackVersion.find().toArray().length === 1) {
        repoVersion = App.StackVersion.find().findProperty('isOutOfSync');
       }
       if (!repoVersion) {
        console.error("CLUSTER STACK VERSIONS ERROR: multiple clusters in OUT_OF_SYNC state OR none in CURRENT or OUT_OF_SYNC state");
        return;
       }
       hostNamesSkipped = this._getSkippedForPassiveStateHosts(hosts, repoVersion);
     }

    var message = "";
    if (operationData.componentNameFormatted) {
      message = Em.I18n.t('hosts.bulkOperation.confirmation.hostComponents').format(operationData.message, operationData.componentNameFormatted, hostNames.length);
    } else {
      message = Em.I18n.t('hosts.bulkOperation.confirmation.hosts').format(operationData.message, hostNames.length);
    }


    return App.ModalPopup.show({
      header: Em.I18n.t('hosts.bulkOperation.confirmation.header'),
      hostNames: hostNames.join("\n"),
      visibleHosts: self._showHostNames(hostNames, "\n", 3),
      hostNamesSkippedVisible: self._showHostNames(hostNamesSkipped, "\n", 3),
      expanded: false,

      hostNamesSkipped: function() {
        return hostNamesSkipped.length ? hostNamesSkipped.join("\n") : false;
      }.property(),

      didInsertElement: function() {
        this._super();
        this.set('expanded', hostNames.length <= 3);
      },
      onPrimary: function() {
        self.bulkOperation(operationData, hosts);
        this._super();
      },

      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/bulk_operation_confirm_popup'),
        message: message,
        textareaVisible: false,

        warningInfo: function() {
          switch (operationData.action) {
            case "DECOMMISSION":
              return Em.I18n.t('hosts.bulkOperation.warningInfo.body');
            case "PASSIVE_STATE":
              return operationData.state === 'OFF' ? Em.I18n.t('hosts.passiveMode.popup.version.mismatch.multiple')
                .format(repoVersion.get('repositoryVersion.repositoryVersion')) : "";
            default:
              return ""
          }
        }.property(),

        textTrigger: function() {
          this.toggleProperty('textareaVisible');
        },

        showAll: function() {
          this.set('parentView.visibleHosts', this.get('parentView.hostNames'));
          this.set('parentView.hostNamesSkippedVisible', this.get('parentView.hostNamesSkipped'));
          this.set('parentView.expanded', true);
        },

        putHostNamesToTextarea: function() {
          var hostNames = this.get('parentView.hostNames');
          if (this.get('textareaVisible')) {
            var wrapper = $(".task-detail-log-maintext");
            $('.task-detail-log-clipboard').html(hostNames).width(wrapper.width()).height(250);
            Em.run.next(function() {
              $('.task-detail-log-clipboard').select();
            });
          }
        }.observes('textareaVisible')

      })
    });
  },

  /**
   * @param {object} json
   * @param {object[]} hosts
   * @param {object} operationData
   * @returns {string[]}
   * @private
   * @method _getSkippedForDecommissionHosts
   */
  _getSkippedForDecommissionHosts: function (json, hosts, operationData) {
    var hostComponentStatusMap = {}; // "DATANODE_c6401.ambari.apache.org" => "STARTED"
    var hostComponentIdMap = {}; // "DATANODE_c6401.ambari.apache.org" => "DATANODE"
    if (json.items) {
      json.items.forEach(function(host) {
        if (host.host_components) {
          host.host_components.forEach(function(component) {
            hostComponentStatusMap[component.id] = component.HostRoles.state;
            hostComponentIdMap[component.id] = component.HostRoles.component_name;
          });
        }
      });
    }
    return hosts.filter(function(host) {
      return host.hostComponents.filter(function(component) {
        return hostComponentIdMap[component] == operationData.realComponentName && hostComponentStatusMap[component] == 'INSTALLED';
      }).length > 0;
    }).mapProperty('hostName');
  },

  /**
   * Exclude <code>outOfSyncHosts</code> hosts for PASSIVE request
   *
   * @param {object[]} hosts
   * @returns {string[]}
   * @private
   * @method _getSkippedForPassiveStateHosts
   */
  _getSkippedForPassiveStateHosts: function (hosts, repoVersion) {
    var hostNames = hosts.mapProperty('hostName');
    var hostNamesSkipped = [];
    var outOfSyncHosts = repoVersion.get('outOfSyncHosts') || [];
    for (var i = 0; i < outOfSyncHosts.length; i++) {
      if (hostNames.contains(outOfSyncHosts[i])) {
        hostNamesSkipped.push(outOfSyncHosts[i]);
      }
    }
    return hostNamesSkipped;
  }

});
