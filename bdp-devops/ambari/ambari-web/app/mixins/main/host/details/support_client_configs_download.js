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

App.SupportClientConfigsDownload = Em.Mixin.create({

  /**
   * This object is supposed to be used as an enum  for resource types supported by ambari
   */
  resourceTypeEnum: Object.freeze({
    CLUSTER: "ClusterResource",
    HOST: "HostResource",
    SERVICE: "ServiceResource",
    SERVICE_COMPONENT: "ServiceComponentResource",
    HOST_COMPONENT: "HostComponentResource"
  }),

  /**
   *
   * @param {{hostName: string, componentName: string, displayName: string, serviceName: string, resourceType: resourceTypeEnum}} data
   */
  downloadClientConfigsCall: function (data) {
    var url = this._getUrl(data.hostName, data.serviceName, data.componentName, data.resourceType);
    var newWindow = window.open(url);
    newWindow.focus();
  },

  /**
   *
   * @param {string|null} hostName
   * @param {string} serviceName
   * @param {string} componentName
   * @param {string} resourceType
   * @returns {string}
   * @private
   */
  _getUrl: function (hostName, serviceName, componentName, resourceType) {
    var result;
    var prefix = App.get('apiPrefix') + '/clusters/' + App.router.getClusterName() + '/';

    switch (resourceType) {
      case this.resourceTypeEnum.SERVICE_COMPONENT:
        result = prefix + 'services/' + serviceName + '/components/' + componentName;
        break;
      case this.resourceTypeEnum.HOST_COMPONENT:
        result = prefix + 'hosts/' + hostName + '/host_components/' + componentName;
        break;
      case this.resourceTypeEnum.HOST:
        result = prefix + 'hosts/' + hostName + '/host_components';
        break;
      case this.resourceTypeEnum.SERVICE:
        result = prefix + 'services/' + serviceName + '/components';
        break;
      case this.resourceTypeEnum.CLUSTER:
      default:
        result = prefix + 'components';
    }

    result += '?format=client_config_tar';
    return result;
  }

});