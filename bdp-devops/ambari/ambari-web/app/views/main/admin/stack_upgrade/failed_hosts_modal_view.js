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


App.FailedHostsPopupBodyView = Em.View.extend({

  templateName: require('templates/main/admin/stack_upgrade/failed_hosts_modal'),

  /**
   * @type {number}
   * @const
   */
  MAX_HOSTNAME_LENGTH: 50,

  /**
   * @type {string}
   */
  subHeader: Em.computed.i18nFormat('admin.stackUpgrade.failedHosts.subHeader', 'parentView.content.hosts.length'),

  didInsertElement: function () {
    App.tooltip(this.$("[rel='UsageTooltip']"));
    this.$(".panel").on("show hide", function (e) {
      $(e.target).siblings(".panel-heading").find("i.panel-toggle").toggleClass('icon-caret-right icon-caret-down')
    });
  },

  /**
   * @type {Array.<Em.Object>}
   */
  hosts: function () {
    var content = this.get('parentView.content');
    var result = [];

    content.hosts.forEach(function (hostName, index) {
      var hostComponents = [];

      if (content.host_detail[hostName]) {
        content.host_detail[hostName].forEach(function (details) {
          hostComponents.push(Em.Object.create({
            componentName: App.format.role(details.component, false),
            serviceName: App.format.role(details.service, true)
          }))
        }, this);
      }
      result.push(Em.Object.create({
        hostName: hostName,
        displayName: hostName.length > this.MAX_HOSTNAME_LENGTH ? hostName.substr(0, this.MAX_HOSTNAME_LENGTH) + '...' : hostName,
        collapseId: 'collapse' + index,
        collapseHref: '#collapse' + index,
        hostComponents: hostComponents
      }))
    }, this);
    return result;
  }.property('parentView.content'),

  /**
   * open hosts info in new window in JSON format
   */
  openDetails: function () {
    var newDocument = window.open().document;
    newDocument.write(JSON.stringify(this.get('parentView.content')));
    newDocument.close();
  }
});
