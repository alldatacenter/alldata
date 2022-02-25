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
var model;

function getModel() {
  return App.RepositoryVersion.createRecord();
}

describe('App.RepositoryVersion', function () {

  beforeEach(function () {
    model = getModel();
  });

  App.TestAliases.testAsComputedFirstNotBlank(getModel(), 'status', ['stackVersion.state', 'defaultStatus']);

  App.TestAliases.testAsComputedIfThenElse(getModel(), 'noInitHostsTooltip', 'noInitHosts', Em.I18n.t('admin.stackVersions.version.emptyHostsTooltip'), Em.I18n.t('admin.stackVersions.version.hostsTooltip'));

  App.TestAliases.testAsComputedIfThenElse(getModel(), 'noCurrentHostsTooltip', 'noCurrentHosts', Em.I18n.t('admin.stackVersions.version.emptyHostsTooltip'), Em.I18n.t('admin.stackVersions.version.hostsTooltip'));

  App.TestAliases.testAsComputedIfThenElse(getModel(), 'noInstalledHostsTooltip', 'noInstalledHosts', Em.I18n.t('admin.stackVersions.version.emptyHostsTooltip'), Em.I18n.t('admin.stackVersions.version.hostsTooltip'));

  describe("#notInstalledHosts", function() {

    before(function () {
      sinon.stub(App, 'get').returns(['host1']);
    });

    beforeEach(function () {
      model = getModel();
    });

    after(function () {
      App.get.restore();
    });

    it("all states empty", function() {
      model.set('stackVersion', Em.Object.create({
        installedHosts: [],
        notInstalledHosts: [],
        currentHosts: []
      }));
      model.propertyDidChange('notInstalledHosts');
      expect(model.get('notInstalledHosts')).to.eql(['host1']);
    });

    it("stackVersion has notInstalledHosts array", function() {
      model.set('stackVersion', Em.Object.create({
        installedHosts: [],
        notInstalledHosts: ['host2'],
        currentHosts: []
      }));
      model.propertyDidChange('notInstalledHosts');
      expect(model.get('notInstalledHosts')).to.eql(['host2']);
    });
  });

});
