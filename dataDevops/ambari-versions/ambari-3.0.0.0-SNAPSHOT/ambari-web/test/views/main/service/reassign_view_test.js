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
require('views/main/service/reassign_view');
var testHelpers = require('test/helpers');

var view;

describe('App.ReassignMasterView', function () {
  beforeEach(function() {
    view = App.ReassignMasterView.create({
      controller: Em.Object.create({
        content: Em.Object.create(),
        setDBProperty: Em.K
      })
    });
  });
  describe('#willInsertElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'loadHosts');
      view.willInsertElement();
    });
    afterEach(function() {
      view.loadHosts.restore();
    });

    it('loadHosts should be called', function() {
      expect(view.loadHosts.calledOnce).to.be.true;
    });
    it('isLoaded should be false', function() {
      expect(view.get('isLoaded')).to.be.false;
    });
  });


  describe('#loadHosts', function() {

    it('App.ajax.send should be called', function() {
      view.loadHosts();
      var args = testHelpers.findAjaxRequest('name', 'hosts.high_availability.wizard');
      expect(args[0]).to.be.eql({
        name: 'hosts.high_availability.wizard',
        data: {},
        sender: view,
        success: 'loadHostsSuccessCallback',
        error: 'loadHostsErrorCallback'
      });
    });
  });

  describe('#loadHostsSuccessCallback', function() {
    var data = {
      items: [
        {
          Hosts: {
            host_name: 'host1'
          }
        }
      ]
    };
    beforeEach(function() {
      sinon.stub(view.get('controller'), 'setDBProperty');
      view.loadHostsSuccessCallback(data);
    });
    afterEach(function() {
      view.get('controller').setDBProperty.restore();
    });

    it('setDBProperty should be called', function() {
      expect(view.get('controller').setDBProperty.calledWith('hosts',
        {
          "host1": {
            "bootStatus": "REGISTERED",
            "isInstalled": true,
            "name": "host1"
          }
        }
      )).to.be.true;
    });
    it('should set hosts to content', function() {
      expect(view.get('controller.content.hosts')).to.be.eql({
        "host1": {
          "bootStatus": "REGISTERED",
          "isInstalled": true,
          "name": "host1"
        }
      });
    });
    it('isLoaded should be true', function() {
      expect(view.get('isLoaded')).to.be.true;
    });
  });

  describe('#loadHostsErrorCallback', function() {

    it('isLoaded should be true', function() {
      view.loadHostsErrorCallback();
      expect(view.get('isLoaded')).to.be.true;
    });
  });
});
