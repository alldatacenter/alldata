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

App.ManageJournalNodeWizardStep3View = App.HighAvailabilityWizardStep4View.extend({
  templateName: require('templates/main/admin/highAvailability/journalNode/step3'),

  didInsertElement: function () {
    this.get('controller').pullCheckPointsStatuses();
  },

  step3BodyText: function () {
    const nN = this.get('controller.content.activeNN'),
      hdfsUser = this.get('controller.content.hdfsUser'),
      nameSpaces = App.HDFSService.find('HDFS').get('masterComponentGroups').mapProperty('name');
    let formatArgs = [nN.host_name];
    if (nameSpaces.length > 1) {
      formatArgs.push(
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.safeModeText'),
        nameSpaces.map(ns => {
          return Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.safeModeCommand')
            .format(hdfsUser, ns);
        }).join('<br>'),
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.checkPointText'),
        nameSpaces.map(ns => {
          return Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.checkPointCommand')
            .format(hdfsUser, ns);
        }).join('<br>'),
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.proceed'),
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.recentCheckPoint')
      );
    } else {
      formatArgs.push(
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.safeModeText'),
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.safeModeCommand').format(hdfsUser),
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.checkPointText'),
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.checkPointCommand').format(hdfsUser),
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.proceed'),
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.recentCheckPoint')
      );
    }
    return Em.I18n.t('admin.manageJournalNode.wizard.step3.body').format(...formatArgs);
  }.property('controller.content.masterComponentHosts', 'controller.isHDFSNameSpacesLoaded'),

  nnCheckPointText: function () {
    if (this.get('controller.isHDFSNameSpacesLoaded')) {
      const nameSpaces = App.HDFSService.find('HDFS').get('masterComponentGroups');
      let key;
      if (nameSpaces.length > 1) {
        key = this.get('controller.isNextEnabled')
          ? 'admin.manageJournalNode.wizard.step3.checkPointsCreated'
          : 'admin.manageJournalNode.wizard.step3.checkPointsNotCreated';
      } else {
        key = this.get('controller.isNextEnabled')
          ? 'admin.highAvailability.wizard.step4.ckCreated'
          : 'admin.highAvailability.wizard.step4.ckNotCreated';
      }
      return Em.I18n.t(key);
    } else {
      return '';
    }
  }.property('controller.isHDFSNameSpacesLoaded', 'controller.isNextEnabled'),

  errorText: function () {
    if (this.get('controller.isHDFSNameSpacesLoaded')) {
      const nameSpaces = App.HDFSService.find('HDFS').get('masterComponentGroups');
      if (nameSpaces.length > 1) {
        return this.get('controller.isActiveNameNodesStarted') ? ''
          : Em.I18n.t('admin.manageJournalNode.wizard.step3.error.multipleNameSpaces.nameNodes');
      } else {
        return this.get('controller.isNameNodeStarted') ? ''
          : Em.I18n.t('admin.highAvailability.wizard.step4.error.nameNode');
      }
    } else {
      return '';
    }
  }.property('controller.isHDFSNameSpacesLoaded', 'controller.isNameNodeStarted', 'controller.isActiveNameNodesStarted')
});
