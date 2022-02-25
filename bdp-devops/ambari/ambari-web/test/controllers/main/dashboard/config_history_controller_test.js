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
require('controllers/main/dashboard/config_history_controller');

describe('MainConfigHistoryController', function () {

  var controller;

  beforeEach(function () {
    controller = App.MainConfigHistoryController.create();
  });

  afterEach(function () {
    clearTimeout(controller.get('timeoutRef'));
    controller.destroy();
  });

  describe('#realUrl', function () {
    it('cluster name is empty', function () {
      App.set('clusterName', '');
      expect(controller.get('realUrl')).to.equal('/api/v1/clusters//configurations/service_config_versions?<parameters>fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true');
    });
    it('cluster name is "mycluster"', function () {
      App.set('clusterName', 'mycluster');
      expect(controller.get('realUrl')).to.equal('/api/v1/clusters/mycluster/configurations/service_config_versions?<parameters>fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true');
    });
  });
  describe('#load()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'updateTotalCounter', Em.K);
      sinon.stub(controller, 'loadConfigVersionsToModel').returns({done: Em.clb});
      controller.load(true);
    });

    afterEach(function () {
      controller.updateTotalCounter.restore();
      controller.loadConfigVersionsToModel.restore();
    });

    it('updateTotalCounter is called once', function () {
      expect(controller.updateTotalCounter.calledOnce).to.be.true;
    });
  });
  describe('#loadConfigVersionsToModel()', function () {

    beforeEach(function () {
      sinon.stub(App.HttpClient, 'get', Em.K);
      sinon.stub(controller, 'getUrl', Em.K);
      sinon.stub(controller, 'getQueryParameters', function(){
        return [1];
      });
      controller.loadConfigVersionsToModel();
    });

    afterEach(function () {
      controller.getUrl.restore();
      controller.getQueryParameters.restore();
      App.HttpClient.get.restore();
    });

    it('HttpClient.get is called once', function () {
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });

    it('getQueryParameters is called once', function () {
      expect(controller.getQueryParameters.calledOnce).to.be.true;
    });

    it('getUrl is called with correct data', function () {
      expect(controller.getUrl.calledWith([1])).to.be.true;
    });
  });

  describe('#updateTotalCounter()', function () {

    beforeEach(function () {
      controller.updateTotalCounter();
    });

    it('ajax-request is sent', function () {
      var args = testHelpers.findAjaxRequest('name', 'service.serviceConfigVersions.get.total');
      expect(args).to.exists;
    });
  });

  describe('#updateTotalCounterSuccess()', function () {
    it('totalCount is updated', function () {
      controller.updateTotalCounterSuccess({itemTotal: 1});
      expect(controller.get('totalCount')).to.equal(1);
    });
  });
  describe('#getUrl()', function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get', function () {
        return {
          computeParameters: function () {
            return 'params'
          }
        }
      });
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it('query params is empty', function () {

      expect(controller.getUrl()).to.equal('/api/v1/clusters/mycluster/configurations/service_config_versions?fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true');
    });
    it('query params is correct', function () {
      expect(controller.getUrl({})).to.equal('/api/v1/clusters/mycluster/configurations/service_config_versions?params&fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true');
    });
  });

  describe('#subscribeToUpdates', function() {
    beforeEach(function() {
      sinon.stub(App.StompClient, 'addHandler');
    });
    afterEach(function() {
      App.StompClient.addHandler.restore();
    });

    it('App.StompClient.subscribe should be called', function() {
      controller.subscribeToUpdates();
      expect(App.StompClient.addHandler.calledWith('/events/configs', 'history')).to.be.true;
    });
  });

  describe('#unsubscribeOfUpdates', function() {
    beforeEach(function() {
      sinon.stub(App.StompClient, 'removeHandler');
    });
    afterEach(function() {
      App.StompClient.removeHandler.restore();
    });

    it('App.StompClient.subscribe should be called', function() {
      controller.unsubscribeOfUpdates();
      expect(App.StompClient.removeHandler.calledWith('/events/configs', 'history')).to.be.true;
    });
  });
  
  describe('#colPropAssoc', function() {
    
    it('should return associations', function() {
      expect(controller.get('colPropAssoc')[1]).to.be.equal('serviceVersion');
      expect(controller.get('colPropAssoc')[2]).to.be.equal('configGroup');
      expect(controller.get('colPropAssoc')[3]).to.be.equal('createTime');
      expect(controller.get('colPropAssoc')[4]).to.be.equal('author');
      expect(controller.get('colPropAssoc')[5]).to.be.equal('notes');
    });
  });
  
  describe('#getSortProps', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getSortingStatuses').returns([
        {
          name: 'serviceVersion',
          status: 'sorting_asc'
        },
        {
          name: 'configGroup',
          status: 'sorting_desc'
        },
        {
          name: 's3',
          status: 'sorting_asc'
        }
      ]);
    });
    afterEach(function() {
      App.db.getSortingStatuses.restore();
    });
    
    it('should return sort properties', function() {
      controller.set('sortProps', [
        {
          name: 'serviceVersion'
        },
        {
          name: 'configGroup'
        },
        {
          name: 's3'
        }
      ]);
      expect(controller.getSortProps()).to.be.eql([
        {
          "key": "service_name.asc,service_config_version",
          "name": "serviceVersion",
          "type": "SORT",
          "value": "desc"
      
        },
        {
          "key": "group_name.desc,service_config_version",
          "name": "configGroup",
          "type": "SORT",
          "value": "desc"
        },
        {
          "name": "s3",
          "type": "SORT",
          "value": "asc"
        }
      ]);
    });
  });
  
  describe('#getSearchBoxSuggestions', function() {
  
    beforeEach(function() {
      App.ajax.send.restore();
      sinon.stub(App.ajax, 'send').returns({
        done: function(callback) {
          callback({items: [{'name1': '1'}]});
          return {
            fail: Em.clb
          }
        }
      });
    });
    
    it('request should be sent', function() {
      controller.set('filterProps', [{
        name: 'name1',
        key: 'key1'
      }]);
      controller.getSearchBoxSuggestions('name1');
      expect(testHelpers.findAjaxRequest('name', 'service.serviceConfigVersions.get.suggestions')[0]).to.be.eql({
        name: 'service.serviceConfigVersions.get.suggestions',
        sender: controller,
        data: {
          'key': 'key1'
        }
      });
    });
  });
});

