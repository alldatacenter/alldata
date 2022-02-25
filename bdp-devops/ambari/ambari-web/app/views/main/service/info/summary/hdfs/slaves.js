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

App.HDFSSlaveComponentsView = Em.View.extend({
  templateName: require('templates/main/service/info/summary/hdfs/slaves'),

  showTitle: false,

  // should be bound to App.MainDashboardServiceHdfsView instance
  summaryView: null,

  dataNodeComponent: Em.Object.create({
    componentName: 'DATANODE'
  }),

  journalNodeComponent: Em.Object.create({
    componentName: 'JOURNALNODE'
  }),

  nfsGatewayComponent: Em.Object.create({
    componentName: 'NFS_GATEWAY'
  }),
  
  hdfsClientComponent: Em.Object.create({
    componentName: 'HDFS_CLIENT'
  }),

  service: Em.computed.alias('summaryView.service'),

  isDataNodeCreated: function () {
    return this.get('summaryView') ? this.get('summaryView').isServiceComponentCreated('DATANODE') : false;
  }.property('App.router.clusterController.isComponentsStateLoaded'),

  isJournalNodeCreated: function () {
    return this.get('summaryView') ? this.get('summaryView').isServiceComponentCreated('JOURNALNODE') : false;
  }.property('App.router.clusterController.isComponentsStateLoaded'),
  
  isHDFSClientCreated: function () {
    return this.get('summaryView') ? this.get('summaryView').isServiceComponentCreated('HDFS_CLIENT') : false;
  }.property('App.router.clusterController.isComponentsStateLoaded'),

  journalNodesLive: function () {
    return this.get('service.journalNodes').filterProperty('workStatus', 'STARTED').get('length');
  }.property('service.journalNodes.@each.workStatus'),

  journalNodesTotal: Em.computed.alias('service.journalNodes.length'),

  /**
   * Define if NFS_GATEWAY is present in the installed stack
   * @type {Boolean}
   */
  isNfsInStack: function () {
    return App.StackServiceComponent.find().someProperty('componentName', 'NFS_GATEWAY');
  }.property()
});

