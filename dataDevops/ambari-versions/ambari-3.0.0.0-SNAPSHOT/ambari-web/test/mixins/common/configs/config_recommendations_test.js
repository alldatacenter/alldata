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

describe('App.ConfigRecommendations', function() {
  var mixinObject = Em.Controller.extend(App.ConfigRecommendations, {});
  var instanceObject = mixinObject.create({});

  beforeEach(function() {
    instanceObject.set('recommendations', []);
  });

  describe('#applyRecommendation', function() {
    beforeEach(function() {
      sinon.stub(instanceObject, 'formatParentProperties', function(parentProperties) { return parentProperties} );
      sinon.stub(App.config, 'get').withArgs('serviceByConfigTypeMap').returns({
        'pFile': Em.Object.create({serviceName: 'sName', displayName: 'sDisplayName'})
      });
      sinon.stub(App.configsCollection, 'getConfigByName').withArgs('pName', 'pFile').returns({
        displayName: 'pDisplayName',
        description: 'pDescription'
      });
      sinon.stub(Handlebars, 'SafeString');
    });
    afterEach(function() {
      instanceObject.formatParentProperties.restore();
      App.config.get.restore();
      App.configsCollection.getConfigByName.restore();
      Handlebars.SafeString.restore();
    });

    it('adds new recommendation', function() {
      var res = instanceObject.applyRecommendation('pName', 'pFile', 'pGroup', 'pRecommended', 'pInitial', ['p_id'], true);
      expect(res).to.eql({
        saveRecommended: true,
        saveRecommendedDefault: true,
        propertyFileName: 'pFile',
        propertyName: 'pName',
        propertyTitle: 'pDisplayName<br><small>pName</small>',
        propertyDescription: 'pDescription',
        isDeleted: false,
        notDefined: false,
        configGroup: 'pGroup',
        initialValue: 'pInitial',
        parentConfigs: ['p_id'],
        serviceName: 'sName',
        allowChangeGroup: false,
        serviceDisplayName: 'sDisplayName',
        recommendedValue: 'pRecommended',
        isEditable: true
      });
      expect(instanceObject.getRecommendation('pName', 'pFile', 'pGroup')).to.eql(res);
    });

    it('updates recommendation', function() {
      instanceObject.set('recommendations', [{
        saveRecommended: true,
        saveRecommendedDefault: true,
        propertyFileName: 'pFile',
        propertyName: 'pName',
        isDeleted: false,
        notDefined: false,
        configGroup: 'pGroup',
        initialValue: 'pInitial',
        parentConfigs: ['p_id'],
        serviceName: 'sName',
        allowChangeGroup: false,
        serviceDisplayName: 'sDisplayName',
        recommendedValue: 'pRecommended'
      }]);
      expect(instanceObject.applyRecommendation('pName', 'pFile', 'pGroup', 'pRecommended1', 'pInitial', ['p_id1'])).to.eql({
        saveRecommended: true,
        saveRecommendedDefault: true,
        propertyFileName: 'pFile',
        propertyName: 'pName',
        isDeleted: false,
        notDefined: false,
        configGroup: 'pGroup',
        initialValue: 'pInitial',
        parentConfigs: ['p_id1', 'p_id'],
        serviceName: 'sName',
        allowChangeGroup: false,
        serviceDisplayName: 'sDisplayName',
        recommendedValue: 'pRecommended1'
      });
    });
  });

  describe('#formatParentProperties', function() {
    beforeEach(function() {
      sinon.stub(App.config, 'configId', function(a,b) { return a + b; });
    });
    afterEach(function() {
      App.config.configId.restore();
    });

    it('returns empty array if nothing was passed', function() {
      expect(instanceObject.formatParentProperties(null)).to.eql([]);
    });

    it('returns config ids array', function() {
      expect(instanceObject.formatParentProperties([{name: "n1", type: "t1"}, {name: "n2", type: "t2"}])).to.eql(["n1t1", "n2t2"]);
    });
  });

  describe('#addRecommendation', function() {
    var cases = [
      {
        title: 'add recommendation for editable property with full info',
        name: 'pName', file: 'pFile.xml', group: 'pGroup', recommended: 'pRecommended', initial: 'pInitial', parent: ['p_id'], isEditable: true,
        service: Em.Object.create({serviceName: 'sName', displayName: 'sDisplayName'}),
        result: {
          saveRecommended: true,
          saveRecommendedDefault: true,
          propertyFileName: 'pFile',
          propertyName: 'pName',
          propertyTitle: 'pDisplayName<br><small>pName</small>',
          propertyDescription: 'pDescription',
          isDeleted: false,
          notDefined: false,
          configGroup: 'pGroup',
          initialValue: 'pInitial',
          parentConfigs: ['p_id'],
          serviceName: 'sName',
          allowChangeGroup: false,
          serviceDisplayName: 'sDisplayName',
          recommendedValue: 'pRecommended',
          isEditable: true
        }
      },
      {
        title: 'add recommendation for read-only property with full info',
        name: 'pName', file: 'pFile.xml', group: 'pGroup', recommended: 'pRecommended', initial: 'pInitial', parent: ['p_id'], isEditable: false,
        service: Em.Object.create({serviceName: 'sName', displayName: 'sDisplayName'}),
        result: {
          saveRecommended: true,
          saveRecommendedDefault: true,
          propertyFileName: 'pFile',
          propertyName: 'pName',
          propertyTitle: 'pDisplayName<br><small>pName</small>',
          propertyDescription: 'pDescription',
          isDeleted: false,
          notDefined: false,
          configGroup: 'pGroup',
          initialValue: 'pInitial',
          parentConfigs: ['p_id'],
          serviceName: 'sName',
          allowChangeGroup: false,
          serviceDisplayName: 'sDisplayName',
          recommendedValue: 'pRecommended',
          isEditable: false
        }
      },
      {
        title: 'add recommendation with min info',
        name: 'pName', file: 'pFile.xml',
        service: Em.Object.create({serviceName: 'sName', displayName: 'sDisplayName'}),
        result: {
          saveRecommended: true,
          saveRecommendedDefault: true,
          propertyFileName: 'pFile',
          propertyName: 'pName',
          propertyTitle: 'pDisplayName<br><small>pName</small>',
          propertyDescription: 'pDescription',
          isDeleted: true,
          notDefined: true,
          configGroup: 'Default',
          initialValue: undefined,
          parentConfigs: [],
          serviceName: 'sName',
          allowChangeGroup: false,
          serviceDisplayName: 'sDisplayName',
          recommendedValue: undefined,
          isEditable: true
        }
      }
    ];
    cases.forEach(function(c) {
      describe('successful add recommendation', function() {
        var recommendation;
        beforeEach(function() {
          instanceObject.set('recommendations', []);
          sinon.stub(App.config, 'get').withArgs('serviceByConfigTypeMap').returns({
            'pFile': c.service
          });
          sinon.stub(Handlebars, 'SafeString');
          sinon.stub(App.configsCollection, 'getConfigByName').withArgs('pName', 'pFile.xml').returns({
            displayName: 'pDisplayName',
            description: 'pDescription'
          });
          recommendation = instanceObject.addRecommendation(c.name, c.file, c.group, c.recommended, c.initial, c.parent, c.isEditable);
        });

        afterEach(function() {
          App.config.get.restore();
          Handlebars.SafeString.restore();
          App.configsCollection.getConfigByName.restore();
        });

        it(c.title, function() {
          expect(recommendation).to.eql(c.result);
        });

        it(c.title + ' check recommendations collection', function() {
          expect(instanceObject.get('recommendations.0')).to.eql(c.result);
        });
      })
    });

    it('throw exception when name, fileName', function() {
      expect(instanceObject.addRecommendation.bind()).to.throw(Error, 'name and fileName should be defined');
      expect(instanceObject.addRecommendation.bind(null, 'fname')).to.throw(Error, 'name and fileName should be defined');
      expect(instanceObject.addRecommendation.bind('name', null)).to.throw(Error, 'name and fileName should be defined');
    });
  });

  describe('#removeRecommendationObject', function () {
    var recommendations = [
      {
        propertyName: 'p1',
        propertyFileName: 'f1'
      },
      {
        propertyName: 'p2',
        propertyFileName: 'f2'
      }
    ];

    beforeEach(function () {
      instanceObject.set('recommendations', recommendations);
    });

    it('remove recommendation', function () {
      instanceObject.removeRecommendationObject(recommendations[1]);

      expect(instanceObject.get('recommendations.length')).to.equal(1);
      expect(instanceObject.get('recommendations.0')).to.eql({
        propertyName: 'p1',
        propertyFileName: 'f1'
      });
    });

    it('remove recommendation that is not exist (don\'t do anything)', function () {
      instanceObject.removeRecommendationObject({propertyName: 'any', 'propertyFileName': 'aby'});
      expect(instanceObject.get('recommendations')).to.eql(recommendations);
    });

    it('throw error if recommendation is undefined ', function () {
      expect(instanceObject.removeRecommendationObject.bind()).to.throw(Error, 'recommendation should be defined object');
      expect(instanceObject.removeRecommendationObject.bind(null)).to.throw(Error, 'recommendation should be defined object');
    });

    it('throw error if recommendation is not an object ', function () {
      expect(instanceObject.removeRecommendationObject.bind('recommendation')).to.throw(Error, 'recommendation should be defined object');
      expect(instanceObject.removeRecommendationObject.bind(['recommendation'])).to.throw(Error, 'recommendation should be defined object');
    });
  });

  describe('#updateRecommendation', function () {
    it('update recommended value and parent properties', function () {
      expect(instanceObject.updateRecommendation({'recommendedValue': 'v2', parentConfigs: ['id1']}, 'v1', ['id2']))
        .to.eql({'recommendedValue': 'v1', parentConfigs: ['id2', 'id1']});
    });

    it('update recommended value and add parent properties', function () {
      expect(instanceObject.updateRecommendation({}, 'v1', ['id1'])).to.eql({'recommendedValue': 'v1', parentConfigs: ['id1']});
    });

    it('update recommended value', function () {
      expect(instanceObject.updateRecommendation({}, 'v1')).to.eql({'recommendedValue': 'v1'});
      expect(instanceObject.updateRecommendation({'recommendedValue': 'v1'}, 'v2')).to.eql({'recommendedValue': 'v2'});
    });

    it('throw error if recommendation is undefined ', function () {
      expect(instanceObject.updateRecommendation.bind()).to.throw(Error, 'recommendation should be defined object');
      expect(instanceObject.updateRecommendation.bind(null)).to.throw(Error, 'recommendation should be defined object');
    });

    it('throw error if recommendation is not an object ', function () {
      expect(instanceObject.updateRecommendation.bind('recommendation')).to.throw(Error, 'recommendation should be defined object');
      expect(instanceObject.updateRecommendation.bind(['recommendation'])).to.throw(Error, 'recommendation should be defined object');
    });
  });

  describe('#saveRecommendation', function() {

    it('skip update since values are same', function() {
      expect(instanceObject.saveRecommendation({saveRecommended: false, saveRecommendedDefault: false}, false)).to.be.false;
    });

    it('perform update since values are different', function() {
      expect(instanceObject.saveRecommendation({saveRecommended: false, saveRecommendedDefault: false}, true)).to.be.true;
    });

    it('updates "saveRecommended" and "saveRecommendedDefault", set "false"', function() {
      var res = {saveRecommended: true, saveRecommendedDefault: true};
      instanceObject.saveRecommendation(res, false);
      expect(res.saveRecommended).to.be.false;
      expect(res.saveRecommendedDefault).to.be.false;
    });

    it('throw error if recommendation is undefined ', function () {
      expect(instanceObject.updateRecommendation.bind()).to.throw(Error, 'recommendation should be defined object');
      expect(instanceObject.updateRecommendation.bind(null)).to.throw(Error, 'recommendation should be defined object');
    });

    it('throw error if recommendation is not an object ', function () {
      expect(instanceObject.updateRecommendation.bind('recommendation')).to.throw(Error, 'recommendation should be defined object');
      expect(instanceObject.updateRecommendation.bind(['recommendation'])).to.throw(Error, 'recommendation should be defined object');
    });
  });

  describe('#getRecommendation', function () {
    var recommendations = [
      {
        propertyName: 'p1',
        propertyFileName: 'f1',
        configGroup: 'Default'
      },
      {
        propertyName: 'p2',
        propertyFileName: 'f2',
        configGroup: 'group1'
      },
      {
        propertyName: 'p1',
        propertyFileName: 'f1',
        configGroup: 'group1'
      }
    ];

    beforeEach(function () {
      instanceObject.set('recommendations', recommendations);
    });

    it('get recommendation for default group', function () {
      expect(instanceObject.getRecommendation('p1', 'f1')).to.eql(recommendations[0]);
    });

    it('get recommendation for default group (2)', function () {
      expect(instanceObject.getRecommendation('p1', 'f1', 'group1')).to.eql(recommendations[2]);
    });

    it('get recommendation for wrong group', function () {
      expect(instanceObject.getRecommendation('p2', 'f2', 'group2')).to.equal(null);
    });

    it('get undefined recommendation', function () {
      expect(instanceObject.getRecommendation('some', 'amy')).to.equal(null);
    });

    it('get throw error if undefined name or fileName passed', function () {
      expect(instanceObject.getRecommendation.bind()).to.throw(Error, 'name and fileName should be defined');
      expect(instanceObject.getRecommendation.bind('name')).to.throw(Error, 'name and fileName should be defined');
      expect(instanceObject.getRecommendation.bind(null, 'fileName')).to.throw(Error, 'name and fileName should be defined');
    });
  });

  describe('#cleanUpRecommendations', function() {
    var cases = [
      {
        title: 'remove recommendations with same init and recommended values',
        recommendations: [{
          initialValue: 'v1', recommendedValue: 'v1', serviceName: 'test', propertyName: 'test', propertyFileName: 'test'
        }, {
            initialValue: 'v1', recommendedValue: 'v2', serviceName: 'test', propertyName: 'test', propertyFileName: 'test'
        }],
        cleanUpRecommendations: [{
          initialValue: 'v1', recommendedValue: 'v2', serviceName: 'test', propertyName: 'test', propertyFileName: 'test'
        }]
      },
      {
        title: 'remove recommendations with null init and recommended values',
        recommendations: [{
          initialValue: null, recommendedValue: null, serviceName: 'test', propertyName: 'test', propertyFileName: 'test'
        }, {
          recommendedValue: null, serviceName: 'test', propertyName: 'test', propertyFileName: 'test'
        }, {
          initialValue: null
        },{
          initialValue: null, recommendedValue: 'v1', serviceName: 'test', propertyName: 'test', propertyFileName: 'test'
        }, {
          initialValue: 'v1', recommendedValue: null, serviceName: 'test', propertyName: 'test', propertyFileName: 'test'
        }],
        cleanUpRecommendations: [{
          initialValue: null, recommendedValue: 'v1', serviceName: 'test', propertyName: 'test', propertyFileName: 'test'
        }, {
          initialValue: 'v1', recommendedValue: null, serviceName: 'test', propertyName: 'test', propertyFileName: 'test'
        }
        ]
      }
    ];

    cases.forEach(function(c) {
      describe(c.title, function() {
        beforeEach(function() {
          instanceObject.set('recommendations', c.recommendations);
          instanceObject.set('stepConfigs', [Em.Object.create({serviceName: 'test',  configs: []})]);
          sinon.stub(App.config, 'configId').returns('test__test');
          instanceObject.cleanUpRecommendations();

        });
        afterEach(function(){
          App.config.configId.restore();
        });
        it('do clean up', function() {
          expect(instanceObject.get('recommendations')).to.eql(c.cleanUpRecommendations);
        });
      });
    });
  });

  describe('#clearRecommendationsByServiceName', function () {
    beforeEach(function () {
      instanceObject.set('recommendations', [{serviceName: 's1'}, {serviceName: 's2'}, {serviceName: 's3'}]);
    });

    it('remove with specific service names ', function () {
      instanceObject.clearRecommendationsByServiceName(['s2','s3']);
      expect(instanceObject.get('recommendations')).to.eql([{serviceName: 's1'}]);
    });
  });

  describe('#clearAllRecommendations', function () {
    beforeEach(function () {
      instanceObject.set('recommendations', [{anyObject: 'o1'}, {anyObject: 'o2'}]);
    });

    it('remove all recommendations', function () {
      instanceObject.clearAllRecommendations();
      expect(instanceObject.get('recommendations.length')).to.equal(0);
    });
  });
});

