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

describe('App.ThemesMappingMixin', function () {
  var mixin;

  beforeEach(function () {
    mixin = Em.Object.create(App.ThemesMappingMixin, {});
  });
  
  describe('#loadConfigTheme', function() {
    beforeEach(function() {
      sinon.stub(App.Tab, 'find').returns([{
        serviceName: 'S2'
      }]);
      sinon.stub(App, 'get').returns('stack1');
    });
    afterEach(function() {
      App.Tab.find.restore();
      App.get.restore();
    });
    
    it('App.ajax.send should be called', function() {
      mixin.loadConfigTheme('S1');
      expect(testHelpers.findAjaxRequest('name', 'configs.theme')[0].data).to.be.eql({
        serviceName: 'S1',
        stackVersionUrl: 'stack1'
      });
    });
  });
  
  describe('#_saveThemeToModel', function() {
    beforeEach(function() {
      sinon.stub(App.themesMapper, 'map');
    });
    afterEach(function() {
      App.themesMapper.map.restore();
    });
    
    it('App.themesMapper.map should be called', function() {
      mixin._saveThemeToModel({}, {}, {serviceName: 'S1'});
      expect(App.themesMapper.map.calledWith({}, ['S1'])).to.be.true;
    });
  });
  
  describe('#loadConfigThemeForServices', function() {
    beforeEach(function() {
      sinon.stub(App, 'get').returns('stack1');
    });
    afterEach(function() {
      App.get.restore();
    });
    
    it('App.ajax.send should be called', function() {
      mixin.loadConfigThemeForServices(['S1', 'S2']);
      expect(testHelpers.findAjaxRequest('name', 'configs.theme.services')[0].data).to.be.eql({
        serviceNames: 'S1,S2',
        stackVersionUrl: 'stack1'
      });
    });
  });
  
  describe('#_loadConfigThemeForServicesSuccess', function() {
    beforeEach(function() {
      sinon.stub(App.themesMapper, 'map');
    });
    afterEach(function() {
      App.themesMapper.map.restore();
    });
    
    it('App.themesMapper.map should be called', function() {
      mixin._loadConfigThemeForServicesSuccess(
        {items: [{themes: [[]]}]},
        {},
        {serviceNames: 'S1,S2'});
      expect(App.themesMapper.map.calledWith(
        {
          items: [[]]
        },
        ['S1', 'S2']
      )).to.be.true;
    });
  });
});
