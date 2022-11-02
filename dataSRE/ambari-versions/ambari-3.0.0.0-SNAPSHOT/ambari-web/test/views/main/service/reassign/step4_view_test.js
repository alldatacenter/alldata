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
require('views/main/service/reassign/step4_view');

var view;

function getView() {
  return App.ReassignMasterWizardStep4View.create();
}

describe('App.ReassignMasterWizardStep4View', function () {
  
  beforeEach(function() {
    view = getView();
  });
  
  describe('#noticeCompleted', function() {
    
    it('should return notice for manual steps', function() {
      view.set('controller', {
        content: {
          hasManualSteps: true,
          reassign: {
            component_name: 'C1'
          }
        }
      });
      view.propertyDidChange('noticeCompleted');
      expect(view.get('noticeCompleted')).to.be.equal(
        Em.I18n.t('services.reassign.step4.status.success.withManualSteps').format('c1')
      );
    });
  
    it('should return notice', function() {
      view.set('controller', {
        content: {
          hasManualSteps: false,
          reassign: {
            component_name: 'C1'
          },
          reassignHosts: {
            source: 'host1',
            target: 'host2'
          }
        }
      });
      view.propertyDidChange('noticeCompleted');
      expect(view.get('noticeCompleted')).to.be.equal(
        Em.I18n.t('services.reassign.step4.status.success').format('c1', 'host1', 'host2')
      );
    });
  });
});
