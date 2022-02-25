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

App.ManageJournalNodeWizardStep5View = Em.View.extend({

  templateName: require('templates/main/admin/highAvailability/journalNode/step5'),

  step5BodyText: function () {
    const existingJournalNode = this.get('controller.content.masterComponentHosts').find(hc => {
        return hc.component === 'JOURNALNODE' && hc.isInstalled;
      }),
      nameSpaces = App.HDFSService.find('HDFS').get('masterComponentGroups').mapProperty('name'),
      hdfsSiteConfigs = this.get('controller.content.serviceConfigProperties.items').findProperty('type', 'hdfs-site'),
      configProperties = hdfsSiteConfigs ? hdfsSiteConfigs.properties : {},
      directories = nameSpaces.length > 1
        ? nameSpaces.map(ns => configProperties[`dfs.journalnode.edits.dir.${ns}`]).uniq()
        : [configProperties['dfs.journalnode.edits.dir']],
      directoriesString = directories.map(dir => `<b>${dir}</b>`).join(', ');
    return Em.I18n.t('admin.manageJournalNode.wizard.step5.body').format(existingJournalNode.hostName, directoriesString);
  }.property('controller.content.masterComponentHosts', 'controller.isHDFSNameSpacesLoaded')
});
