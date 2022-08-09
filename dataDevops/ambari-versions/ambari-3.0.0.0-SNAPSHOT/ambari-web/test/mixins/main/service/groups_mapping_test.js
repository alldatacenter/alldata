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

describe('App.GroupsMappingMixin', function () {
  var mixin;

  beforeEach(function () {
    mixin = Em.Object.create(App.GroupsMappingMixin, {});
  });
  
  describe('#loadConfigGroups', function() {
    beforeEach(function() {
      sinon.stub(mixin, 'trackRequest');
    });
    afterEach(function() {
      mixin.trackRequest.restore();
    });
    
    it('configGroupsAreLoaded should be true', function() {
      mixin.loadConfigGroups([]);
      expect(mixin.get('configGroupsAreLoaded')).to.be.true;
    });
  
    it('App.ajax.send should be called', function() {
      mixin.loadConfigGroups(['S1', 'S2']);
      expect(testHelpers.findAjaxRequest('name', 'configs.config_groups.load.services')[0]).to.exists;
    });
  });
  
  describe('#saveConfigGroupsToModel', function() {
    var dfd = {resolve: sinon.spy()};
    
    beforeEach(function() {
      sinon.stub(App.configGroupsMapper, 'map');
      mixin.saveConfigGroupsToModel({}, {}, {serviceNames: 'S1,S2', dfd: dfd});
    });
    afterEach(function() {
      App.configGroupsMapper.map.restore();
    });
    
    it('App.configGroupsMapper.map should be called', function() {
      expect(App.configGroupsMapper.map.calledWith({}, false, ['S1', 'S2'])).to.be.true;
    });
  
    it('configGroupsAreLoaded should be true', function() {
      expect(mixin.get('configGroupsAreLoaded')).to.be.true;
    });
  
    it('resolve should be called', function() {
      expect(dfd.resolve.called).to.be.true;
    });
  });
});
