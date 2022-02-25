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

App.ReassignMasterWizardStep5View = Em.View.extend({

  templateName: require('templates/main/service/reassign/step5'),

  manualCommands: function () {
    const componentName = this.get('controller.content.reassign.component_name');
    if (!this.get('controller.content.componentsWithManualCommands').contains(componentName)) {
      return '';
    }
    var
      atsDir = App.StackService.find('YARN').compareCurrentVersion('2.7') > -1 ? "timeline-state-store.ldb" : "leveldb-timeline-store.ldb",
      componentDir = this.get('controller.content.componentDir') || '',
      componentDirCmd = componentDir.replace(/,/g, ' '),
      sourceHost = this.get('controller.content.reassignHosts.source'),
      targetHost = this.get('controller.content.reassignHosts.target'),
      ha = '',
      user = this.get('controller.content.hdfsUser'), path;

    if (componentName === 'NAMENODE' && App.get('isHaEnabled')) {
      ha = '_ha';
      var nnStartedHost = this.get('controller.content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName').without(sourceHost).without(targetHost);
    }

    if (this.get('controller.content.reassign.component_name') === 'APP_TIMELINE_SERVER') {
      user = this.get('controller.content.configs.yarn-env.yarn_user');
      path = this.get('controller.content.configs.yarn-site')['yarn.timeline-service.leveldb-timeline-store.path'];
    }

    return Em.I18n.t('services.reassign.step5.body.' + this.get('controller.content.reassign.component_name').toLowerCase() + ha).
      format(componentDir, sourceHost, targetHost, user, nnStartedHost,this.get('controller.content.group'), componentDirCmd, path, atsDir);
  }.property('controller.content.reassign.component_name', 'controller.content.componentDir', 'controller.content.masterComponentHosts', 'controller.content.reassign.host_id', 'controller.content.hdfsUser'),

  /**
   * security notice to generate keytab manually is not used any more
   */
  securityNotice: function () {
    var secureConfigs = this.get('controller.content.secureConfigs');
    var proceedMsg = Em.I18n.t('services.reassign.step5.body.proceedMsg');
    var hasSecureConfigs = !this.get('controller.content.componentsWithoutSecurityConfigs').contains(this.get('controller.content.reassign.component_name'));
    if(!hasSecureConfigs) {
      secureConfigs = [];
    }
    if (!App.get('isKerberosEnabled') || !secureConfigs.length) {
      return proceedMsg;
    }
    var formattedText = '<ul>';
    secureConfigs.forEach(function (config) {
      formattedText += '<li>' + Em.I18n.t('services.reassign.step5.body.securityConfigsList').format(config.keytab,
          config.principal.replace('_HOST', this.get('controller.content.reassignHosts.target')), this.get('controller.content.reassignHosts.target')) + '</li>';
    }, this);
    formattedText += '</ul>';
    return Em.I18n.t('services.reassign.step5.body.securityNotice').format(formattedText) + proceedMsg;
  }.property('App.isKerberosEnabled','controller.content.secureConfigs', 'controller.content.reassignHosts.target')
});
