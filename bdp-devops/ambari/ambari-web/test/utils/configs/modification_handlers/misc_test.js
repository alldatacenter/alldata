/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


require('utils/configs/modification_handlers/modification_handler');

var App = require('app');

describe('#App.ServiceConfigModificationHandler MISC', function() {
  var genStepConfig = function(serviceName, properties) {
    return Em.Object.create({
      serviceName: serviceName,
      configs: properties
    });
  };
  var genProperty = function(propName, value, initialValue, filename) {
    return Em.Object.create({
      name: propName,
      value: value,
      initialValue: initialValue,
      filename: filename
    });
  };
  var genProperties = function(properties) {
    return properties.map(function(item) {
      return genProperty.apply(undefined, item);
    });
  };
  var handler = require('utils/configs/modification_handlers/misc');

  describe('#getDependentConfigChanges', function() {

    describe('YARN dependent configs', function() {
      var tests = [
        {
          miscHandlerParams: {
            changedConfig: genProperty('yarn_user', 'new-user', 'initial-user', 'yarn-env.xml'),
            selectedServices: ['HDFS', 'YARN', 'MAPREDUCE2', 'ZOOKEEPER'],
            allConfigs: [
              genStepConfig('YARN', genProperties([
                ['yarn.admin.acl', 'some-user2 some-group2', '', 'yarn-site.xml']
              ])),
              genStepConfig('MISC', genProperties([
                ['user_group', 'some-group', 'initial-group', 'cluster-env.xml'],
                ['yarn_user', 'new-user', 'initial-user', 'yarn-env.xml']
              ]))
            ]
          },
          m: 'yarn_user changed, yarn.admin.acl new user name should be appended to users joined joined comma',
          e: { propertyName: 'yarn.admin.acl', curValue: 'some-user2 some-group2', newValue: 'some-user2,new-user some-group2'}
        },
        {
          miscHandlerParams: {
            changedConfig: genProperty('yarn_user', 'new-user', 'initial-user', 'yarn-env.xml'),
            selectedServices: ['HDFS', 'YARN', 'MAPREDUCE2', 'ZOOKEEPER'],
            allConfigs: [
              genStepConfig('YARN', genProperties([
                ['yarn.admin.acl', 'initial-user some-group2', '', 'yarn-site.xml']
              ])),
              genStepConfig('MISC', genProperties([
                ['user_group', 'some-group', 'initial-group', 'cluster-env.xml'],
                ['yarn_user', 'some-user', 'initial-user', 'yarn-env.xml']
              ]))
            ]
          },
          m: 'yarn_user changed, yarn.admin.acl initial user name should be update with new one',
          e: { propertyName: 'yarn.admin.acl', curValue: 'initial-user some-group2', newValue: 'new-user some-group2'}
        },
        {
          miscHandlerParams: {
            changedConfig: genProperty('yarn_user', 'new-user', 'initial-user', 'yarn-env.xml'),
            selectedServices: ['HDFS', 'YARN', 'MAPREDUCE2', 'ZOOKEEPER'],
            allConfigs: [
              genStepConfig('YARN', genProperties([
                ['yarn.admin.acl', '', '', 'yarn-site.xml']
              ])),
              genStepConfig('MISC', genProperties([
                ['user_group', 'some-group', 'initial-group', 'cluster-env.xml'],
                ['yarn_user', 'new-user', 'initial-user', 'yarn-env.xml']
              ]))
            ]
          },
          m: 'yarn_user changed, yarn.admin.acl initial value is not in valid format its value should be set with yarn_user and user_group',
          e: { propertyName: 'yarn.admin.acl', curValue: '', newValue: 'new-user'}
        }
      ];

      tests.forEach(function(test) {
        it(test.m, function() {
          var handlerParams = test.miscHandlerParams;
          var result = handler.getDependentConfigChanges(handlerParams.changedConfig, handlerParams.selectedServices, handlerParams.allConfigs, false).toArray();
          // check the key => value according test.e from result
          expect(App.permit(result.findProperty('propertyName', test.e.propertyName), Em.keys(test.e))).to.eql(test.e);
        });
      });
    });
  });
});
