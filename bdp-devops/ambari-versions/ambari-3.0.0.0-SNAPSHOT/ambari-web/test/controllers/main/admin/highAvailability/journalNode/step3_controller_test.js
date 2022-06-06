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

describe('App.ManageJournalNodeWizardStep3Controller', function () {
  var controller;

  beforeEach(function () {
    controller = App.ManageJournalNodeWizardStep3Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#pullCheckPointsStatuses', function () {
    it('should removeObserver if HDFS namespaces are loaded', function () {
      controller.set('isHDFSNameSpacesLoaded', true);
      controller.set('content.masterComponentHosts', [{component: 'NAMENODE', isInstalled: true, hostName: 'test'}]);
      sinon.stub(controller, 'addObserver');
      sinon.stub(controller, 'removeObserver');
      controller.pullCheckPointsStatuses();
      expect(controller.addObserver.calledOnce).to.be.false;
      expect(controller.removeObserver.calledWith('isHDFSNameSpacesLoaded', controller, 'pullCheckPointsStatuses')).to.be.true;
      controller.addObserver.restore();
      controller.removeObserver.restore();
    });

    it('should send ajax request if nameSpacesCount is more than 1', function () {
      sinon.stub(App.HDFSService, 'find').returns(Em.Object.create({
        masterComponentGroups: [{}, {}],
        activeNameNodes: [{hostName: 'test1'}, {hostName: 'test2'}]
      }));
      controller.pullCheckPointsStatuses();
      var args = testHelpers.findAjaxRequest('admin.high_availability.getNnCheckPointsStatuses');
      expect(args[0]).to.be.eql({
        name: 'admin.high_availability.getNnCheckPointsStatuses',
        sender: controller,
        data: {
          hostNames: ['test1', 'test2']
        },
        success: 'checkNnCheckPointsStatuses'
      });
      App.HDFSService.find.restore();
    });

    it('should send ajax request if nameSpacesCount is more than 1 and add started hosts', function () {
      sinon.stub(App.HDFSService, 'find').returns(Em.Object.create({
        masterComponentGroups: [{
          hosts: ['test1']
        }, {
          hosts: ['test2']
        }, {
          hosts: ['test3']
        }],
        activeNameNodes: [{hostName: 'test1'}, {hostName: 'test2'}]
      }));

      controller.pullCheckPointsStatuses();
      var args = testHelpers.findAjaxRequest('admin.high_availability.getNnCheckPointsStatuses');
      expect(args[0].data.hostNames.length).to.be.equal(3);
      App.HDFSService.find.restore();
    });
  });
});