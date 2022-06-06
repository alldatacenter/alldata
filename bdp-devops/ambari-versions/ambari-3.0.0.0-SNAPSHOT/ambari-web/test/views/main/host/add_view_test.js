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
var view;

describe('App.AddHostView', function () {

  beforeEach(function () {
    view = App.AddHostView.create({
      controller: Em.Object.create({
        getDBProperty: Em.K,
        setDBProperty: Em.K,
        content: Em.Object.create()
      })
    });
  });

  describe("#willInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(view, 'loadHosts');
      this.mock = sinon.stub(view.get('controller'), 'getDBProperty');
    });
    afterEach(function() {
      view.loadHosts.restore();
      this.mock.restore();
    });

    it("hosts saved in DB", function() {
      this.mock.returns(['host1']);
      view.willInsertElement();
      expect(view.get('isLoaded')).to.be.true;
      expect(view.loadHosts.calledOnce).to.be.false;
    });
    it("hosts not saved in DB", function() {
      this.mock.returns(null);
      view.willInsertElement();
      expect(view.get('isLoaded')).to.be.false;
      expect(view.loadHosts.calledOnce).to.be.true;
    });
  });

  describe("#loadHosts()", function() {

    it("App.ajax.send should be called", function() {
      view.loadHosts();
      var args = testHelpers.filterAjaxRequests('name', 'hosts.confirmed.minimal');
      expect(args[0][0]).to.eql({
        name: 'hosts.confirmed.minimal',
        sender: view,
        data: {},
        success: 'loadHostsSuccessCallback',
        error: 'loadHostsErrorCallback'
      });
    });
  });

  describe("#loadHostsSuccessCallback()", function() {

    beforeEach(function() {
      sinon.stub(view.get('controller'), 'setDBProperty');
    });
    afterEach(function() {
      view.get('controller').setDBProperty.restore();
    });

    it("should save hosts to DB", function() {
      var response = {items: [
        {
          Hosts: {
            host_name: 'host1'
          },
          host_components: [
            {
              component_name: 'C1'
            }
          ]
        }
      ]};
      view.loadHostsSuccessCallback(response);
      expect(view.get('isLoaded')).to.be.true;
      expect(view.get('controller').setDBProperty.calledWith('hosts', {
        host1: {
          name: 'host1',
          bootStatus: "REGISTERED",
          isInstalled: true,
          hostComponents: [
            {
              component_name: 'C1'
            }
          ]
        }
      })).to.be.true;
      expect(view.get('controller.content.hosts')).to.eql({
        host1: {
          name: 'host1',
          bootStatus: "REGISTERED",
          isInstalled: true,
          hostComponents: [
            {
              component_name: 'C1'
            }
          ]
        }
      });
    });
  });

  describe("#loadHostsErrorCallback()", function() {
    it("isLoaded should be set to true", function() {
      view.loadHostsErrorCallback();
      expect(view.get('isLoaded')).to.be.true;
    });
  });
});