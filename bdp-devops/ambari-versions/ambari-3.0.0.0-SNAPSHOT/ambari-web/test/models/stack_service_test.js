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

require('models/stack_service');

describe('App.StackService', function () {

  App.store.safeLoad(App.StackService, {
    id: 'S1'
  });

  var ss = App.StackService.find('S1');
  ss.reopen({
    serviceComponents: []
  });

  describe('#isDFS', function () {
    it('service name is "SERVICE"', function () {
      ss.set('serviceName', 'SERVICE');
      ss.propertyDidChange('isDFS');
      expect(ss.get('isDFS')).to.be.false;
    });
    it('service name is "HDFS"', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('isDFS');
      expect(ss.get('isDFS')).to.be.true;
    });
    it('service name is "GLUSTERFS"', function () {
      ss.set('serviceName', 'GLUSTERFS');
      ss.propertyDidChange('isDFS');
      expect(ss.get('isDFS')).to.be.true;
    });
  });

  describe('#isPrimaryDFS', function () {
    it('service name is "SERVICE"', function () {
      ss.set('serviceName', 'SERVICE');
      ss.propertyDidChange('isPrimaryDFS');
      expect(ss.get('isPrimaryDFS')).to.be.false;
    });
    it('service name is "HDFS"', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('isPrimaryDFS');
      expect(ss.get('isPrimaryDFS')).to.be.true;
    });
  });

  describe('#configTypesRendered', function () {
    ss.set('configTypes', {
      'core-site': {},
      'hdfs-site': {},
      'oozie-site': {}
    });
    it('service name is "SERVICE"', function () {
      ss.set('serviceName', 'SERVICE');
      ss.propertyDidChange('configTypesRendered');
      expect(ss.get('configTypesRendered')).to.eql({'core-site': {},'hdfs-site': {}, 'oozie-site': {}});
    });
    it('service name is "GLUSTERFS"', function () {
      ss.set('serviceName', 'GLUSTERFS');
      ss.propertyDidChange('configTypesRendered');
      expect(ss.get('configTypesRendered')).to.eql({'core-site': {},'hdfs-site': {}, 'oozie-site': {}});
    });
    it('service name is "HDFS"', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('configTypesRendered');
      expect(ss.get('configTypesRendered')).to.eql({'core-site': {}, 'hdfs-site': {}, 'oozie-site': {}});
    });
    it('service name is "FALCON"', function () {
      ss.set('serviceName', 'FALCON');
      ss.propertyDidChange('configTypesRendered');
      expect(ss.get('configTypesRendered')).to.eql({'core-site': {}, 'hdfs-site': {}});
    });
  });

  describe('#displayNameOnSelectServicePage', function () {
    it('No coSelectedServices', function () {
      ss.set('serviceName', 'HDFS');
      ss.set('displayName', 'HDFS');
      ss.propertyDidChange('displayNameOnSelectServicePage');
      expect(ss.get('displayNameOnSelectServicePage')).to.equal('HDFS');
    });
    it('Present coSelectedServices', function () {
      ss.reopen({
        coSelectedServices: ['MAPREDUCE2']
      });
      ss.set('serviceName', 'YARN');
      ss.set('displayName', 'YARN');
      ss.propertyDidChange('displayNameOnSelectServicePage');
      expect(ss.get('displayNameOnSelectServicePage')).to.equal('YARN + MapReduce2');
    });
  });

  describe('#isHiddenOnSelectServicePage', function () {
    var testCases = [
      {
        serviceName: 'HDFS',
        isInstallable: true,
        result: false
      },
      {
        serviceName: 'KERBEROS',
        isInstallable: false,
        result: true
      }
    ];

    testCases.forEach(function (test) {
      it('service name - ' + test.serviceName, function () {
        ss.set('serviceName', test.serviceName);
        ss.set('isInstallable', test.isInstallable);
        ss.propertyDidChange('isHiddenOnSelectServicePage');
        expect(ss.get('isHiddenOnSelectServicePage')).to.equal(test.result);
      });
    });
  });

  describe('#isMonitoringService', function () {
    var testCases = [
      {
        serviceName: 'HDFS',
        result: false
      },
      {
        serviceName: 'GANGLIA',
        result: true
      }
    ];

    testCases.forEach(function (test) {
      it('service name - ' + test.serviceName, function () {
        ss.set('serviceName', test.serviceName);
        ss.propertyDidChange('isMonitoringService');
        expect(ss.get('isMonitoringService')).to.equal(test.result);
      });
    });
  });

  describe('#hasClient', function () {
    it('No client serviceComponents', function () {
      ss.set('serviceComponents', []);
      ss.propertyDidChange('hasClient');
      expect(ss.get('hasClient')).to.be.false;
    });
    it('Has client serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({isClient: true})]);
      ss.propertyDidChange('hasClient');
      expect(ss.get('hasClient')).to.be.true;
    });
  });

  describe('#hasMaster', function () {
    it('No master serviceComponents', function () {
      ss.set('serviceComponents', []);
      ss.propertyDidChange('hasMaster');
      expect(ss.get('hasMaster')).to.be.false;
    });
    it('Has master serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({isMaster: true})]);
      ss.propertyDidChange('hasMaster');
      expect(ss.get('hasMaster')).to.be.true;
    });
  });

  describe('#hasSlave', function () {
    it('No slave serviceComponents', function () {
      ss.set('serviceComponents', []);
      ss.propertyDidChange('hasSlave');
      expect(ss.get('hasSlave')).to.be.false;
    });
    it('Has slave serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({isSlave: true})]);
      ss.propertyDidChange('hasSlave');
      expect(ss.get('hasSlave')).to.be.true;
    });
  });

  describe('#hasNonMastersWithCustomAssignment', function () {
    it('No serviceComponents', function () {
      ss.set('serviceComponents', []);
      ss.propertyDidChange('hasNonMastersWithCustomAssignment');
      expect(ss.get('hasNonMastersWithCustomAssignment')).to.be.false;
    });
    it('All non-master serviceComponents are required on all hosts', function () {
      ss.set('serviceComponents', [Em.Object.create({isMaster: true}), Em.Object.create({isSlave: true, cardinality: 'ALL'}), Em.Object.create({isClient: true, cardinality: 'ALL'})]);
      ss.propertyDidChange('hasNonMastersWithCustomAssignment');
      expect(ss.get('hasNonMastersWithCustomAssignment')).to.be.false;
    });
    it('Has non-master serviceComponents not required on all hosts', function () {
      ss.set('serviceComponents', [Em.Object.create({isSlave: true}), Em.Object.create({isClient: true})]);
      ss.propertyDidChange('hasNonMastersWithCustomAssignment');
      expect(ss.get('hasNonMastersWithCustomAssignment')).to.be.true;
    });
  });

  App.TestAliases.testAsComputedEveryBy(ss, 'isClientOnlyService', 'serviceComponents', 'isClient', true);

  describe('#isNoConfigTypes', function () {
    it('configTypes is null', function () {
      ss.set('configTypes', null);
      ss.propertyDidChange('isNoConfigTypes');
      expect(ss.get('isNoConfigTypes')).to.be.true;
    });
    it('configTypes is empty', function () {
      ss.set('configTypes', {});
      ss.propertyDidChange('isNoConfigTypes');
      expect(ss.get('isNoConfigTypes')).to.be.true;
    });
    it('configTypes is correct', function () {
      ss.set('configTypes', {'key': {}});
      ss.propertyDidChange('isNoConfigTypes');
      expect(ss.get('isNoConfigTypes')).to.be.false;
    });
  });

  describe('#customReviewHandler', function () {
    it('service name is HDFS', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('customReviewHandler');
      expect(ss.get('customReviewHandler')).to.be.undefined;
    });
    it('service name is HIVE', function () {
      ss.set('serviceName', 'HIVE');
      ss.propertyDidChange('customReviewHandler');
      expect(ss.get('customReviewHandler')).to.eql({
        "Database": "loadHiveDbValue"
      });
    });
  });

  describe('#configCategories', function () {
    it('HDFS service with no serviceComponents', function () {
      ss.set('serviceComponents', []);
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('configCategories');
      expect(ss.get('configCategories').mapProperty('name')).to.eql([
        "General",
        "Advanced",
        "Advanced key"
      ]);
    });
    it('HDFS service with DATANODE serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({componentName: 'DATANODE'})]);
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('configCategories');
      expect(ss.get('configCategories').mapProperty('name')).to.eql([
        "DATANODE",
        "General",
        "Advanced",
        "Advanced key"]);
    });

    it('HDFS service with custom serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({componentName: 'DATANODE'})]);
      ss.set('serviceName', 'HDFS');
      ss.set('configTypes', { key: { supports: {adding_forbidden: "false"}}});
      ss.propertyDidChange('configCategories');
      expect(ss.get('configCategories').mapProperty('name')).to.eql([
        "DATANODE",
        "General",
        "Advanced",
        "Advanced key",
        "Custom key"]);
    });
  });

  describe('#isDisabled', function () {

    var cases = [
      {
        isInstalled: true,
        isMandatory: true,
        clusterInstallCompleted: true,
        isDisabled: true
      },
      {
        isInstalled: true,
        isMandatory: true,
        clusterInstallCompleted: false,
        isDisabled: true
      },
      {
        isInstalled: true,
        isMandatory: false,
        clusterInstallCompleted: true,
        isDisabled: true
      },
      {
        isInstalled: true,
        isMandatory: false,
        clusterInstallCompleted: false,
        isDisabled: true
      },
      {
        isInstalled: false,
        isMandatory: true,
        clusterInstallCompleted: true,
        isDisabled: false
      },
      {
        isInstalled: false,
        isMandatory: true,
        clusterInstallCompleted: false,
        isDisabled: true
      },
      {
        isInstalled: false,
        isMandatory: false,
        clusterInstallCompleted: true,
        isDisabled: false
      },
      {
        isInstalled: false,
        isMandatory: false,
        clusterInstallCompleted: false,
        isDisabled: false
      }
    ];

    cases.forEach(function (testCase) {

      var title = 'isInstalled: {0}, isMandatory: {1}, clusterInstallCompleted: {2}, isDisabled: {3}'
        .format(testCase.isInstalled, testCase.isMandatory, testCase.clusterInstallCompleted, testCase.isDisabled);

      it(title, function () {
        ss.setProperties({
          isInstalled: testCase.isInstalled,
          isMandatory: testCase.isMandatory
        });
        App.set('router.clusterInstallCompleted', testCase.clusterInstallCompleted);
        expect(ss.get('isDisabled')).to.equal(testCase.isDisabled);
      });

    });

  });


});
