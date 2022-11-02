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
  return App.ServiceConfigGroup.createRecord({
    parentConfigGroup: Em.Object.create({
      hosts: []
    })
  });
}

describe('App.ServiceConfigGroup', function () {

  beforeEach(function () {
    model = getModel();
  });

  describe("#displayName", function() {

    before(function () {
      sinon.stub(App.config, 'truncateGroupName');
    });
    after(function () {
      App.config.truncateGroupName.restore();
    });

    it("App.config.truncateGroupName should be called", function() {
      model.set('name', 'group1');
      model.get('displayName');
      expect(App.config.truncateGroupName.called).to.be.true;
    });
  });

  describe("#availableHosts", function() {

    it("default group", function() {
      model.reopen({
        isDefault: true
      });
      expect(model.get('availableHosts')).to.be.empty;
    });

    it("no cluster hosts", function() {
      model.reopen({
        isDefault: false,
        clusterHosts: []
      });
      expect(model.get('availableHosts')).to.be.empty;
    });

    it("cluster hosts used", function() {
      model.reopen({
        isDefault: false,
        clusterHosts: [
          Em.Object.create({id: 'g1'})
        ]
      });
      expect(model.get('availableHosts')).to.be.empty;
    });

    describe("cluster hosts not used", function() {
      var host = Em.Object.create({
        id: 'g1',
        hostComponents: [{componentName: 'c1'}]
      });

      beforeEach(function () {
        model.reopen({
          isDefault: false,
          clusterHosts: [host]
        });
        model.set('parentConfigGroup.hosts', ['g1']);
      });
      it('availableHosts is not empty', function () {
        expect(model.get('availableHosts')).to.be.not.empty;
      });
      it('1st host is selected', function () {
        expect(model.get('availableHosts')[0].get('selected')).to.be.false;
      });
      it('1st host components are correct', function () {
        expect(model.get('availableHosts')[0].get('hostComponentNames')).to.eql(['c1']);
      });
      it('1st host `host` is correct', function () {
        expect(model.get('availableHosts')[0].get('host')).to.eql(host);
      });
    });
  });

  describe("#propertiesList", function() {

    it("properties is null", function() {
      model.set('properties', null);
      expect(model.get('propertiesList')).to.be.empty;
    });

    it("properties is correct", function() {
      model.set('properties', [
        {
          name: 'p1',
          value: 'v1'
        }
      ]);
      expect(model.get('propertiesList')).to.equal('p1 : v1<br/>');
    });
  });

  describe("#getParentConfigGroupId()", function () {

    before(function () {
      sinon.stub(App.ServiceConfigGroup, 'groupId');
    });
    after(function () {
      App.ServiceConfigGroup.groupId.restore();
    });

    it("App.ServiceConfigGroup.groupId should be called", function () {
      App.ServiceConfigGroup.getParentConfigGroupId('S1');
      expect(App.ServiceConfigGroup.groupId.calledWith('S1', 'Default')).to.be.true;
    });
  });

  describe("#groupId()", function () {

    it("should return group id", function () {
      expect(App.ServiceConfigGroup.groupId('S1', 'g1')).to.be.equal('S1_g1');
    });
  });
});
