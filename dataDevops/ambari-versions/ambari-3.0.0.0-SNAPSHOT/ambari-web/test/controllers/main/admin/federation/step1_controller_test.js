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
require('controllers/main/admin/highAvailability_controller');
var validator = require('utils/validator');

describe('App.NameNodeFederationWizardStep1Controller', function () {
  var controller;
  beforeEach(function () {
    controller = App.NameNodeFederationWizardStep1Controller.create();
  });

  after(function () {
    controller.destroy();
  });

  describe('#existingNameServices', function () {
    it ('should return empty array when isHostComponentMetricsLoaded is false', function(){
      sinon.stub(App.router, 'get').returns(false);
      controller.propertyDidChange('App.router.clusterController.isHDFSNameSpacesLoaded');
      expect(controller.get('existingNameServices')).to.eql([]);
      App.router.get.restore();
    });

    it ('should return names array when isHostComponentMetricsLoaded is true', function(){
      sinon.stub(App.router, 'get').returns(true);
      sinon.stub(App.HDFSService, 'find').returns([Em.Object.create({
        masterComponentGroups: [{name: 'test1'}, {name: 'test2'}]
      })]);
      controller.propertyDidChange('App.router.clusterController.isHDFSNameSpacesLoaded');
      expect(controller.get('existingNameServices')).to.eql(['test1', 'test2']);
      App.router.get.restore();
      App.HDFSService.find.restore();
    });
  });

  describe('#existingNameServicesString', function () {
    it('should return empty string when existingNameServices is empty', function () {
      controller.set('existingNameServices', []);
      controller.propertyDidChange('existingNameServices');
      expect(controller.get('existingNameServicesString')).to.equal('');
    });

    it('should return string with names separated by comma and space when existingNameServices is not empty', function () {
      sinon.stub(App.router, 'get').returns(true);
      sinon.stub(App.HDFSService, 'find').returns([Em.Object.create({
        masterComponentGroups: [{name: 'test1'}, {name: 'test2'}]
      })]);
      controller.propertyDidChange('App.router.clusterController.isHDFSNameSpacesLoaded');
      controller.propertyDidChange('existingNameServices');
      expect(controller.get('existingNameServicesString')).to.equal('test1, test2');
      App.router.get.restore();
      App.HDFSService.find.restore();
    });
  });

  describe('#isNameServiceIdError', function () {
    it('should return truthly when nameServiceId is false', function () {
      controller.set('content', Em.Object.create({nameServiceId: null}));
      controller.propertyDidChange('content.nameServiceId');
      expect(controller.get('isNameServiceIdError')).to.be.true;
    });

    it('should return truthly when nameServiceId is exists and existingNameServices contains it', function () {
      controller.set('content', Em.Object.create({nameServiceId: 'test1'}));
      sinon.stub(App.router, 'get').returns(true);
      sinon.stub(App.HDFSService, 'find').returns([Em.Object.create({
        masterComponentGroups: [{name: 'test1'}, {name: 'test2'}]
      })]);
      controller.propertyDidChange('App.router.clusterController.isHDFSNameSpacesLoaded');
      controller.propertyDidChange('content.nameServiceId');
      expect(controller.get('isNameServiceIdError')).to.be.true;
      App.router.get.restore();
      App.HDFSService.find.restore();
    });

    it('should return validator result in other cases', function() {
      controller.set('content', Em.Object.create({nameServiceId: 'test3'}));
      sinon.stub(App.router, 'get').returns(true);
      sinon.stub(App.HDFSService, 'find').returns([Em.Object.create({
        masterComponentGroups: [{name: 'test1'}, {name: 'test2'}]
      })]);
      controller.propertyDidChange('App.router.clusterController.isHDFSNameSpacesLoaded');
      controller.propertyDidChange('content.nameServiceId');
      expect(controller.get('isNameServiceIdError')).to.be.false;
      App.router.get.restore();
      App.HDFSService.find.restore();
    });
  });

  describe('#next', function() {
    it('should call router next method if has not error', function() {
      controller.set('content', Em.Object.create({nameServiceId: 'test3'}));
      sinon.stub(App.router, 'get').returns(true);
      sinon.stub(App.HDFSService, 'find').returns([Em.Object.create({
        masterComponentGroups: [{name: 'test1'}, {name: 'test2'}]
      })]);
      sinon.stub(App.router, 'send');
      controller.propertyDidChange('App.router.clusterController.isHDFSNameSpacesLoaded');
      controller.propertyDidChange('content.nameServiceId');
      controller.next();
      expect(App.router.send.calledOnce).to.be.true;
      App.router.get.restore();
      App.HDFSService.find.restore();
      App.router.send.restore();
    });

    it('should not call router next method if has error', function() {
      controller.set('content', Em.Object.create({nameServiceId: 'test1'}));
      sinon.stub(App.router, 'get').returns(true);
      sinon.stub(App.HDFSService, 'find').returns([Em.Object.create({
        masterComponentGroups: [{name: 'test1'}, {name: 'test2'}]
      })]);
      sinon.stub(App.router, 'send');
      controller.propertyDidChange('App.router.clusterController.isHDFSNameSpacesLoaded');
      controller.propertyDidChange('content.nameServiceId');
      controller.next();
      expect(App.router.send.calledOnce).to.be.false;
      App.router.get.restore();
      App.HDFSService.find.restore();
      App.router.send.restore();
    })

  })
});