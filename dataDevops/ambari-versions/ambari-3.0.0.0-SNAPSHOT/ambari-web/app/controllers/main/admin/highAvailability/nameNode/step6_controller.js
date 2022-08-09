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

App.HighAvailabilityWizardStep6Controller = Em.Controller.extend({

  name:"highAvailabilityWizardStep6Controller",

  POLL_INTERVAL: 1000,

  MINIMAL_JOURNALNODE_COUNT: 3,

  /**
   * Define whether next button should be enabled
   * @type {Boolean}
   */
  isNextEnabled: false,

  /**
   * Counter of initialized JournalNodes
   * @type {Number}
   */
  initJnCounter: 0,

  /**
   * Counter of completed requests
   * @type {Number}
   */
  requestsCounter: 0,

  /**
   * Define whether are some not started JournalNodes
   * @type {Boolean}
   */
  hasStoppedJNs: false,

  /**
   * Current status for step.
   * waiting - for waiting for initializing
   * done - for completed initializing check
   * journalnode_stopped - if there are stopped JournalNode
   * @type {String}
   */
  status: 'waiting',

  loadStep: function () {
    this.set('status', 'waiting');
    this.set('isNextEnabled', false);
    this.pullCheckPointStatus();
  },

  pullCheckPointStatus: function () {
    this.set('initJnCounter', 0);
    this.set('requestsCounter', 0);
    this.set('hasStoppedJNs', false);
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', "JOURNALNODE").mapProperty('hostName');
    hostNames.forEach(function (hostName) {
      this.pullEachJnStatus(hostName);
    }, this);
  },

  pullEachJnStatus: function (hostName) {
    return App.ajax.send({
      name: 'admin.high_availability.getJnCheckPointStatus',
      sender: this,
      data: {
        hostName: hostName
      },
      success: 'checkJnCheckPointStatus'
    });
  },

  checkJnCheckPointStatus: function (data) {
    var journalStatusInfo;
    var initJnCounter = 0;

    if (data.metrics && data.metrics.dfs) {
      journalStatusInfo = $.parseJSON(data.metrics.dfs.journalnode.journalsStatus);
      if (journalStatusInfo[this.get('content.nameServiceId')] && journalStatusInfo[this.get('content.nameServiceId')].Formatted === "true") {
        initJnCounter = this.incrementProperty('initJnCounter');
      }
    } else {
      this.set('hasStoppedJNs', true);
    }

    if (this.incrementProperty('requestsCounter') === this.MINIMAL_JOURNALNODE_COUNT) {
      this.resolveJnCheckPointStatus(initJnCounter);
    }
  },

  resolveJnCheckPointStatus: function(initJnCounter) {
    var self = this;
    if (initJnCounter === this.MINIMAL_JOURNALNODE_COUNT) {
      this.set('status', 'done');
      this.set('isNextEnabled', true);
    } else {
      if (this.get('hasStoppedJNs')) {
        this.set('status', 'journalnode_stopped')
      } else {
        this.set('status', 'waiting');
      }
      window.setTimeout(function () {
        self.pullCheckPointStatus();
      }, self.POLL_INTERVAL);
    }
  },

  done: function () {
    if (this.get('isNextEnabled')) {
      App.router.send('next');
    }
  }

});

