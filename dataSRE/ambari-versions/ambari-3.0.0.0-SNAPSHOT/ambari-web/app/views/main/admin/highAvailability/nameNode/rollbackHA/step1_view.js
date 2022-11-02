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

App.RollbackHighAvailabilityWizardStep1View = Em.View.extend({

  templateName: require('templates/main/admin/highAvailability/nameNode/rollbackHA/step1'),

  addNNHosts: null,
  sNNHosts: null,
  selectedSNNHost: null,
  selectedAddNNHost: null,

  isLoaded: false,

  loadHostsName: function () {
    App.ajax.send({
      name: 'hosts.all',
      sender: this,
      data: {},
      success: 'loadHostsNameSuccessCallback',
      error: 'loadHostsNameErrorCallback'
    });
  },

  loadHostsNameSuccessCallback: function (data) {
    var addNNHosts = App.HostComponent.find().filterProperty('componentName', 'NAMENODE');

    this.secondaryNNHosts = [];
    this.set('selectedSNNHost', this.get('controller.content.sNNHost'));
    this.set('selectedAddNNHost', this.get('controller.content.addNNHost'));

    if (addNNHosts.length == 2) {
      this.set('addNNHosts', addNNHosts.mapProperty('hostName'));
    }
    data.items.forEach(function (host) {
      this.secondaryNNHosts.push(host.Hosts.host_name);
    }, this);
    this.set('sNNHosts', this.secondaryNNHosts);
    this.set('isLoaded', true);
  },
  loadHostsNameErrorCallback: function(){
    this.set('isLoaded', true);
  },

  didInsertElement: function() {
    this.loadHostsName();
  },

  tipAddNNHost: Em.computed.alias('controller.content.addNNHost'),

  tipSNNHost: Em.computed.alias('controller.content.sNNHost'),

  done: function () {
    this.get('controller.content').set('selectedSNNHost', this.get('selectedSNNHost'));
    this.get('controller.content').set('selectedAddNNHost', this.get('selectedAddNNHost'));
    App.router.send('next');
  }

});
