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

require('controllers/main/service/reassign/step1_controller');
require('models/host_component');
var testHelpers = require('test/helpers');

describe('App.ReassignMasterWizardStep1Controller', function () {


  var controller = App.ReassignMasterWizardStep1Controller.create({
    content: Em.Object.create({
      reassign: Em.Object.create({}),
      services: []
    })
  });
  controller.set('_super', Em.K);

  describe('#loadConfigsTags', function() {
    beforeEach(function() {
      this.stub = sinon.stub(App.router, 'get');
    });

    afterEach(function() {
      this.stub.restore();
    });

    it('tests loadConfigsTags', function() {
      controller.loadConfigsTags();
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args).exists;
    });

    it('tests saveDatabaseType with type', function() {
      this.stub.returns({ saveDatabaseType: Em.K});

      controller.saveDatabaseType(true);
      expect(App.router.get.calledOnce).to.be.true;
    });

    it('tests saveDatabaseType without type', function() {
      this.stub.returns({ saveDatabaseType: Em.K});

      controller.saveDatabaseType(false);
      expect(App.router.get.called).to.be.false;
    });

    it('tests saveServiceProperties with properties', function() {
      this.stub.returns({ saveServiceProperties: Em.K});

      controller.saveServiceProperties(true);
      expect(App.router.get.calledOnce).to.be.true;
    });

    it('tests saveServiceProperties without properties', function() {
      this.stub.returns({ saveServiceProperties: Em.K});

      controller.saveServiceProperties(false);
      expect(App.router.get.called).to.be.false;
    });

    it('tests getDatabaseHost', function() {
      controller.set('content.configs', {
        'hive-site': {
          'javax.jdo.option.ConnectionURL': 'jdbc:mysql://c6401/hive?createDatabaseIfNotExist=true'

        }
      });

      controller.set('content.reassign.service_id', 'HIVE');
      controller.set('databaseType', 'mysql');

      expect(controller.getDatabaseHost()).to.equal('c6401')
    });

  });

  describe('#onLoadConfigs', function() {

    var reassignCtrl;

    beforeEach(function() {
      controller = App.ReassignMasterWizardStep1Controller.create({
        content: Em.Object.create({
          reassign: Em.Object.create({
            'component_name': 'OOZIE_SERVER',
            'service_id': 'OOZIE'
          }),
          services: []
        })
      });
      controller.set('_super', Em.K);

      sinon.stub(controller, 'getDatabaseHost', Em.K);
      sinon.stub(controller, 'saveDatabaseType', Em.K);
      sinon.stub(controller, 'saveServiceProperties', Em.K);
      sinon.stub(controller, 'saveConfigs', Em.K);
      sinon.stub(controller, 'isExistingDb');

      reassignCtrl = App.router.reassignMasterController;
      reassignCtrl.set('content.hasManualSteps', true);
    });

    afterEach(function() {
      controller.getDatabaseHost.restore();
      controller.saveDatabaseType.restore();
      controller.saveServiceProperties.restore();
      controller.saveConfigs.restore();
      controller.isExistingDb.restore();
    });
  
    it('should not set hasManualSteps to false for oozie with derby db', function() {
      var data = {
        items: [
          {
            type: 'oozie-site',
            properties: {
              'oozie.service.JPAService.jdbc.driver': 'jdbc:derby:${oozie.data.dir}/${oozie.db.schema.name}-db;create=true'
            }
          }
        ]
      };
    
      expect(reassignCtrl.get('content.hasManualSteps')).to.be.true;

      controller.onLoadConfigs(data);

      expect(reassignCtrl.get('content.hasManualSteps')).to.be.true;
    });
  
    it('should set hasManualSteps to false for oozie without derby db', function() {
      var data = {
        items: [
          {
            type: 'oozie-site',
            properties: {
              'oozie.service.JPAService.jdbc.driver': 'mysql'
            }
          }
        ]
      };

    
      expect(reassignCtrl.get('content.hasManualSteps')).to.be.true;

      controller.onLoadConfigs(data);

      expect(reassignCtrl.get('content.hasManualSteps')).to.be.false;
    });
  });

  describe("#getConfigUrlParams()", function() {
    it("unknown component", function() {
      expect(controller.getConfigUrlParams("", {})).to.be.empty;
    });
    it("OOZIE_SERVER component", function() {
      var data = {
        Clusters: {
          desired_configs: {
            'oozie-site': {
              tag: 'tag'
            },
            'oozie-env': {
              tag: 'tag'
            }
          }
        }
      };
      expect(controller.getConfigUrlParams("OOZIE_SERVER", data)).to.eql([
        "(type=oozie-site&tag=tag)",
        "(type=oozie-env&tag=tag)"
      ]);
    });
    it("HIVE_SERVER component", function() {
      var data = {
        Clusters: {
          desired_configs: {
            'hive-site': {
              tag: 'tag'
            },
            'hive-env': {
              tag: 'tag'
            }
          }
        }
      };
      expect(controller.getConfigUrlParams("HIVE_SERVER", data)).to.eql([
        "(type=hive-site&tag=tag)",
        "(type=hive-env&tag=tag)"
      ]);
    });
  });

  describe("#onLoadConfigsTags()", function () {
    beforeEach(function () {
      this.mock = sinon.stub(controller, 'getConfigUrlParams');
    });
    afterEach(function () {
      this.mock.restore();
    });
    it("empty params", function () {
      this.mock.returns([]);
      controller.onLoadConfigsTags();
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args).not.exists;
    });
    it("correct params", function () {
      this.mock.returns(['p1', 'p2']);
      controller.onLoadConfigsTags();
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        urlParams: 'p1|p2'
      });
    });
  });
});
