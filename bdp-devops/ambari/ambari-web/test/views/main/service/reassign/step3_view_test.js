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
require('views/main/service/reassign/step3_view');

var view;

function getView() {
  return App.ReassignMasterWizardStep3View.create();
}

describe('App.ReassignMasterWizardStep3View', function () {
  
  beforeEach(function() {
    view = getView();
  });
  
  describe('#didInsertElement', function() {
    
    it('loadStep should be called', function() {
      view.set('controller', {
        loadStep: sinon.spy()
      });
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });
  });
  
  describe('#jdbcSetupMessage', function() {

    it('should return empty', function() {
      view.propertyDidChange('jdbcSetupMessage');
      expect(view.get('jdbcSetupMessage')).to.be.empty;
    });
  
    it('should return empty for OOZIE SERVER', function() {
      view.set('controller', {
        content: {
          reassign: {
            component_name: 'OOZIE_SERVER'
          },
          databaseType: 'derby'
        }
      });
      view.propertyDidChange('jdbcSetupMessage');
      expect(view.get('jdbcSetupMessage')).to.be.empty;
    });
  
    it('should return empty for OOZIE SERVER', function() {
      view.set('controller', {
        content: {
          reassign: {
            component_name: 'HIVE_SERVER'
          },
          databaseType: 'type1'
        }
      });
      view.propertyDidChange('jdbcSetupMessage');
      expect(view.get('jdbcSetupMessage')).to.be.equal(
        Em.I18n.t('services.service.config.database.msg.jdbcSetup').format('type1', 'type1')
      );
    });
  });
  
});
