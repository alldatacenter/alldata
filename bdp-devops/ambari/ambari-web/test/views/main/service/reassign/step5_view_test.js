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
require('views/main/service/reassign/step5_view');

var view;

function getView() {
  return App.ReassignMasterWizardStep5View.create();
}

describe('App.ReassignMasterWizardStep5View', function () {
  
  beforeEach(function() {
    view = getView();
  });
  
  describe('#manualCommands', function() {
    beforeEach(function() {
      sinon.stub(App.StackService, 'find').returns({
        compareCurrentVersion: function() {return 1;}
      });
      sinon.stub(App, 'get').returns(true);
    });
    afterEach(function() {
      App.StackService.find.restore();
      App.get.restore();
    });
    
    it('should be empty', function() {
      view.set('controller', {
        content: {
          reassign: {
            component_name: 'C1'
          },
          componentsWithManualCommands: []
        }
      });
      view.propertyDidChange('manualCommands');
      expect(view.get('manualCommands')).to.be.empty;
    });
  
    it('NAMENODE component', function() {
      view.set('controller', {
        content: {
          group: 'group1',
          masterComponentHosts: [{
            component: 'NAMENODE',
            hostName: 'host3'
          }],
          hdfsUser: 'hdfs',
          reassign: {
            component_name: 'NAMENODE'
          },
          reassignHosts: {
            source: 'host1',
            target: 'host2'
          },
          componentDir: 'dir',
          componentsWithManualCommands: ['NAMENODE']
        }
      });
      view.propertyDidChange('manualCommands');
      expect(view.get('manualCommands')).to.be.equal(
        Em.I18n.t('services.reassign.step5.body.namenode_ha').
        format('dir', 'host1', 'host2', 'hdfs', 'host3', 'group1', 'dir', undefined, 'timeline-state-store.ldb')
      );
    });
  
    it('APP_TIMELINE_SERVER component', function() {
      view.set('controller', {
        content: {
          configs: {
            'yarn-env': {
              'yarn_user': 'yarn'
            },
            'yarn-site': {
              'yarn.timeline-service.leveldb-timeline-store.path': 'path'
            }
          },
          group: 'group1',
          hdfsUser: 'hdfs',
          reassign: {
            component_name: 'APP_TIMELINE_SERVER'
          },
          reassignHosts: {
            source: 'host1',
            target: 'host2'
          },
          componentDir: 'dir',
          componentsWithManualCommands: ['APP_TIMELINE_SERVER']
        }
      });
      view.propertyDidChange('manualCommands');
      expect(view.get('manualCommands')).to.be.equal(
        Em.I18n.t('services.reassign.step5.body.app_timeline_server').
        format('dir', 'host1', 'host2', 'yarn', 'host3', 'group1', 'dir', 'path', 'timeline-state-store.ldb')
      );
    });
  });
  
  describe('#securityNotice', function() {
    
    beforeEach(function() {
      sinon.stub(App, 'get').returns(true);
    });
    afterEach(function() {
      App.get.restore();
    });
    
    it('no secure configs', function() {
      view.set('controller', {
        content: {
          secureConfigs: [],
          componentsWithoutSecurityConfigs: [],
          reassign: {
            component_name: 'C1'
          }
        }
      });
      view.propertyDidChange('securityNotice');
      expect(view.get('securityNotice')).to.be.equal(Em.I18n.t('services.reassign.step5.body.proceedMsg'));
    });
  
    it('has secure configs', function() {
      view.set('controller', {
        content: {
          secureConfigs: [{
            principal: '_HOST',
            keytab: 'keytab1'
          }],
          componentsWithoutSecurityConfigs: [],
          reassign: {
            component_name: 'C1'
          },
          reassignHosts: {
            target: 'host2',
            source: 'host1'
          }
        }
      });
      view.propertyDidChange('securityNotice');
      expect(view.get('securityNotice')).to.be.equal(
        Em.I18n.t('services.reassign.step5.body.securityNotice').format(
          "<ul><li>" + Em.I18n.t('services.reassign.step5.body.securityConfigsList').format('keytab1','host2', 'host2') + "</li></ul>"
        ) + Em.I18n.t('services.reassign.step5.body.proceedMsg')
      );
    });
  });
  
});
