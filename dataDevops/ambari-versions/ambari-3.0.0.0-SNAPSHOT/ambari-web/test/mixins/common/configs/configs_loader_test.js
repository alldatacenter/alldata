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

var testHelpers = require('test/helpers');

describe('App.ConfigsLoader', function() {
	var mixin;

	beforeEach(function() {
    mixin = Em.Object.create(App.ConfigsLoader, {
      content: Em.Object.create(),
      trackRequest: Em.K,
      parseConfigData: Em.K,
      host: Em.Object.create(),
      clearRecommendationsInfo: Em.K,
      isVersionDefault: Em.K
    });
	});

  describe("#clearLoadInfo()", function () {

    it("allVersionsLoaded should be false", function() {
      mixin.clearLoadInfo();
      expect(mixin.get('allVersionsLoaded')).to.be.false;
    });
  });

  describe("#loadServiceConfigVersions()", function () {

    it("App.ajax.send should be called", function() {
      mixin.set('content.serviceName', 'S1');
      mixin.loadServiceConfigVersions();
      expect(testHelpers.findAjaxRequest('name', 'service.serviceConfigVersions.get')[0]).to.eql({
        name: 'service.serviceConfigVersions.get',
        data: {
          serviceName: 'S1'
        },
        sender: mixin,
        success: 'loadServiceConfigVersionsSuccess'
      });
    });

    it("allVersionsLoaded should be false", function() {
      mixin.loadServiceConfigVersions();
      expect(mixin.get('allVersionsLoaded')).to.be.false;
    });
  });

  describe("#loadServiceConfigVersionsSuccess()", function () {

    beforeEach(function() {
      sinon.stub(App.serviceConfigVersionsMapper, 'map');
      sinon.stub(mixin, 'loadPreSelectedConfigVersion');
    });

    afterEach(function() {
      App.serviceConfigVersionsMapper.map.restore();
      mixin.loadPreSelectedConfigVersion.restore();
    });

    it("data exist", function() {
      mixin.loadServiceConfigVersionsSuccess({items: [{}]});
      expect(App.serviceConfigVersionsMapper.map.calledWith({items: [{}]})).to.be.true;
      expect(mixin.get('currentDefaultVersion')).to.be.null;
    });

    it("data contains current default version", function() {
      var data = {items: [{
        group_name: 'Default',
        is_current: true,
        service_config_version: 'v1'
      }]};
      mixin.loadServiceConfigVersionsSuccess(data);
      expect(App.serviceConfigVersionsMapper.map.calledWith(data)).to.be.true;
      expect(mixin.get('currentDefaultVersion')).to.be.equal('v1');
    });

    it("allVersionsLoaded should be true", function() {
      mixin.loadServiceConfigVersionsSuccess({items: []});
      expect(mixin.get('allVersionsLoaded')).to.be.true;
    });

    it("selectedVersion should be set", function() {
      mixin.set('preSelectedConfigVersion', null);
      mixin.set('currentDefaultVersion', 'v1');
      mixin.loadServiceConfigVersionsSuccess({items: []});
      expect(mixin.get('selectedVersion')).to.be.equal('v1');
    });

    it("preSelectedConfigVersion should be null", function() {
      mixin.loadServiceConfigVersionsSuccess();
      expect(mixin.get('preSelectedConfigVersion')).to.be.null;
    });
  });

  describe("#loadPreSelectedConfigVersion()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'loadConfigGroups', function() {
        return {done: function(callback) {
          callback();
        }}
      });
      sinon.stub(mixin, 'loadSelectedVersion');
      sinon.stub(App.ServiceConfigGroup, 'find').returns([
        Em.Object.create({
          serviceName: 'S1',
          name: 'G1',
          isDefault: true
        })
      ]);
      mixin.set('preSelectedConfigVersion', Em.Object.create({
        serviceName: 'S1',
        groupName: 'G1',
        version: 'v1'
      }));
    });

    afterEach(function() {
      mixin.loadConfigGroups.restore();
      mixin.loadSelectedVersion.restore();
      App.ServiceConfigGroup.find.restore();
    });

    it("loadConfigGroups should be called", function() {
      mixin.set('servicesToLoad', ['S1']);
      mixin.loadPreSelectedConfigVersion();
      expect(mixin.loadConfigGroups.calledWith(['S1'])).to.be.true;
    });

    it("selectedVersion should be set", function() {
      mixin.loadPreSelectedConfigVersion();
      expect(mixin.get('selectedVersion')).to.be.equal('v1');
    });

    it("preselected selectedConfigGroup should be set", function() {
      mixin.loadPreSelectedConfigVersion();
      expect(mixin.get('selectedConfigGroup')).to.be.eql(Em.Object.create({
        serviceName: 'S1',
        name: 'G1',
        isDefault: true
      }));
    });

    it("loadSelectedVersion should be called", function() {
      mixin.loadPreSelectedConfigVersion();
      expect(mixin.loadSelectedVersion.calledOnce).to.be.true;
    });
  });

  describe("#loadCurrentVersions()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'trackRequestChain');
      mixin.set('currentDefaultVersion', {});
      mixin.set('servicesToLoad', ['S1', 'S2']);
      mixin.loadCurrentVersions();
    });

    afterEach(function() {
      mixin.trackRequestChain.restore();
    });

    it("isCompareMode should be false", function() {
      expect(mixin.get('isCompareMode')).to.be.false;
    });

    it("versionLoaded should be false", function() {
      expect(mixin.get('versionLoaded')).to.be.false;
    });

    it("selectedVersion should be set", function() {
      expect(mixin.get('versionLoaded')).to.be.an.object;
    });

    it("preSelectedConfigVersion should be null", function() {
      expect(mixin.get('preSelectedConfigVersion')).to.be.null;
    });

    it("trackRequest should be called", function() {
      expect(mixin.trackRequestChain.calledOnce).to.be.true;
    });

    it("App.ajax.send should be called", function() {
      expect(testHelpers.findAjaxRequest('name', 'service.serviceConfigVersions.get.current')[0]).to.eql({
        name: 'service.serviceConfigVersions.get.current',
        sender: mixin,
        data: {
          serviceNames: 'S1,S2'
        },
        success: 'loadCurrentVersionsSuccess'
      });
    });
  });

  describe("#loadCurrentVersionsSuccess()", function () {
    var group = Em.Object.create({
      isDefault: false,
      hosts: ['host1']
    });
    var defaultGroup = Em.Object.create({
      isDefault: true
    });

    beforeEach(function() {
      sinon.stub(App.configGroupsMapper, 'map');
      sinon.stub(mixin, 'loadConfigGroups').returns({
        done: function(callback) {callback();}
      });
      sinon.stub(App.ServiceConfigGroup, 'find').returns([group, defaultGroup]);
      sinon.stub(mixin, 'parseConfigData');
    });

    afterEach(function() {
      App.configGroupsMapper.map.restore();
      mixin.loadConfigGroups.restore();
      App.ServiceConfigGroup.find.restore();
      mixin.parseConfigData.restore();
    });

    it("App.configGroupsMapper.map should be called", function() {
      mixin.loadCurrentVersionsSuccess({}, {}, {serviceNames: 'S1,S2'});
      expect(App.configGroupsMapper.map.calledWith({}, true, ['S1', 'S2'])).to.be.true;
    });

    it("parseConfigData should be called", function() {
      mixin.loadCurrentVersionsSuccess({}, {}, {serviceNames: 'S1,S2'});
      expect(mixin.parseConfigData.calledWith({})).to.be.true;
    });

    it("isHostsConfigsPage=true, non-default group", function() {
      mixin.set('isHostsConfigsPage', true);
      mixin.set('host.hostName', 'host1');
      mixin.loadCurrentVersionsSuccess({}, {}, {serviceNames: 'S1,S2'});
      expect(mixin.get('selectedConfigGroup')).to.be.eql(group);
    });

    it("isHostsConfigsPage=true, default group", function() {
      mixin.set('isHostsConfigsPage', true);
      mixin.loadCurrentVersionsSuccess({}, {}, {serviceNames: 'S1,S2'});
      expect(mixin.get('selectedConfigGroup')).to.be.eql(defaultGroup);
    });

    it("isHostsConfigsPage=false", function() {
      mixin.set('isHostsConfigsPage', false);
      mixin.loadCurrentVersionsSuccess({}, {}, {serviceNames: 'S1,S2'});
      expect(mixin.get('selectedConfigGroup')).to.be.eql(defaultGroup);
    });
  });

  describe("#loadSelectedVersion()", function () {

    beforeEach(function() {
      mixin.set('currentDefaultVersion', 'v1');
      sinon.stub(mixin, 'clearRecommendationsInfo');
      sinon.stub(mixin, 'loadCurrentVersions');
      sinon.stub(mixin, 'loadDefaultGroupVersion');
    });

    afterEach(function() {
      mixin.clearRecommendationsInfo.restore();
      mixin.loadCurrentVersions.restore();
      mixin.loadDefaultGroupVersion.restore();
    });

    it("isCompareMode should be false", function() {
      mixin.loadSelectedVersion();
      expect(mixin.get('isCompareMode')).to.be.false;
    });

    it("versionLoaded should be false", function() {
      mixin.loadSelectedVersion();
      expect(mixin.get('versionLoaded')).to.be.false;
    });

    it("clearRecommendationsInfo should be called", function() {
      mixin.loadSelectedVersion();
      expect(mixin.clearRecommendationsInfo.calledOnce).to.be.true;
    });

    it("loadDefaultGroupVersion should be called", function() {
      mixin.loadSelectedVersion('v2', {});
      expect(mixin.loadDefaultGroupVersion.calledWith('v2', {})).to.be.true;
    });

    it("loadCurrentVersions should be called", function() {
      mixin.loadSelectedVersion('v1', null);
      expect(mixin.loadCurrentVersions.calledOnce).to.be.true;
      expect(mixin.get('selectedVersion')).to.be.equal('v1');
    });

    it("loadCurrentVersions should be called, default", function() {
      mixin.loadSelectedVersion('v1', Em.Object.create({isDefault: true}));
      expect(mixin.loadCurrentVersions.calledOnce).to.be.true;
      expect(mixin.get('selectedVersion')).to.be.equal('v1');
    });
  });

  describe("#loadDefaultGroupVersion()", function () {

    beforeEach(function() {
      mixin.set('content.serviceName', 'S1');
      this.mock = sinon.stub(mixin, 'isVersionDefault');
      sinon.stub(mixin, 'setSelectedConfigGroup');
    });

    afterEach(function() {
      this.mock.restore();
      mixin.setSelectedConfigGroup.restore();
    });

    it("setSelectedConfigGroup should be called", function() {
      mixin.loadDefaultGroupVersion('v1', {});
      expect(mixin.setSelectedConfigGroup.calledWith('v1', {})).to.be.true;
    });

    it("default version", function() {
      mixin.set('dependentServiceNames', 'S2');
      this.mock.returns(true);
      mixin.loadDefaultGroupVersion('v1', {});
      expect(mixin.get('selectedVersion')).to.be.equal('v1');
      expect(testHelpers.findAjaxRequest('name', 'service.serviceConfigVersions.get.multiple')[0]).to.eql({
        name: 'service.serviceConfigVersions.get.multiple',
        sender: mixin,
        data: {
          serviceName: 'S1',
          serviceConfigVersions: ['v1'],
          additionalParams: '|(service_name.in(S2)%26is_current=true)'
        },
        success: 'loadSelectedVersionsSuccess'
      });
    });

    it("non-default version", function() {
      mixin.set('dependentServiceNames', '');
      mixin.set('currentDefaultVersion', 'v1');
      this.mock.returns(false);
      mixin.loadDefaultGroupVersion('v2', {});
      expect(mixin.get('selectedVersion')).to.be.equal('v2');
      expect(testHelpers.findAjaxRequest('name', 'service.serviceConfigVersions.get.multiple')[0]).to.eql({
        name: 'service.serviceConfigVersions.get.multiple',
        sender: mixin,
        data: {
          serviceName: 'S1',
          serviceConfigVersions: ['v1', 'v2'],
          additionalParams: ''
        },
        success: 'loadSelectedVersionsSuccess'
      });
    });
  });

  describe("#setSelectedConfigGroup()", function () {

    beforeEach(function() {
      mixin.set('configGroups', [{isDefault: true}]);
      mixin.set('selectedConfigGroup', null);
      this.mock = sinon.stub(mixin, 'isVersionDefault');
    });

    afterEach(function() {
      mixin.isVersionDefault.restore();
    });

    var testCases = [
      {
        dataIsLoaded: true,
        isVersionDefault: true,
        switchToGroup: null,
        expected: {isDefault: true}
      },
      {
        dataIsLoaded: true,
        isVersionDefault: false,
        switchToGroup: {},
        expected: {}
      },
      {
        dataIsLoaded: true,
        isVersionDefault: true,
        switchToGroup: null,
        expected: {isDefault: true}
      },
      {
        dataIsLoaded: false,
        isVersionDefault: false,
        switchToGroup: {},
        expected: null
      }
    ];

    testCases.forEach(function(test) {
      it("dataIsLoaded = " +test.dataIsLoaded + " " +
         "isVersionDefault = " + test.isVersionDefault + " " +
         "switchToGroup = "+ test.switchToGroup, function() {
        this.mock.returns(test.isVersionDefault);
        mixin.set('dataIsLoaded', test.dataIsLoaded);
        mixin.setSelectedConfigGroup('v1', test.switchToGroup);
        expect(mixin.get('selectedConfigGroup')).to.be.eql(test.expected);
      });
    });
  });

  describe("#loadSelectedVersionsSuccess()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'parseConfigData');
    });

    afterEach(function() {
      mixin.parseConfigData.restore();
    });

    it("parseConfigData should be called", function() {
      mixin.loadSelectedVersionsSuccess({});
      expect(mixin.parseConfigData.calledWith({})).to.be.true;
    });
  });

});
