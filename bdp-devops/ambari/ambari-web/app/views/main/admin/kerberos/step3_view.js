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

App.KerberosWizardStep3View = App.KerberosProgressPageView.extend({

  templateName: require('templates/main/admin/kerberos/step3'),

  noticeCompleted: Em.I18n.t('admin.kerberos.wizard.step3.notice.completed'),

  submitButtonText: Em.I18n.t('common.next') + '&rarr;',

  showBackButton: true,

  isHostHeartbeatLost: function() {
    return !Em.isEmpty(this.get('controller.heartBeatLostHosts'));
  }.property('controller.heartBeatLostHosts.length'),

  resultMsg: function() {
    if (this.get('isHostHeartbeatLost')) {
      return Em.I18n.t('installer.step9.status.hosts.heartbeat_lost').format(this.get('controller.heartBeatLostHosts.length'));
    }
    return '';
  }.property('isHostHeartbeatLost'),

  showHostsWithLostHeartBeat: function() {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step9.host.heartbeat_lost.header'),
      autoHeight: false,
      secondary: null,
      bodyClass: Em.View.extend({
        hosts: self.get('controller.heartBeatLostHosts'),
        template: Em.Handlebars.compile('{{#each hostName in view.hosts}}<p>{{view.hostName}}</p>{{/each}}')
      })
    });
  }
});
