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
require('views/main/service/reassign/step2_view');

var view;

function getView() {
  return App.ReassignMasterWizardStep2View.create();
}

describe('App.ReassignMasterWizardStep2View', function () {
  
  beforeEach(function() {
    view = getView();
  });
  
  describe('#alertMessage', function() {
    beforeEach(function() {
      sinon.stub(App, 'get').returns(true);
    });
    afterEach(function() {
      App.get.restore();
    });
    
    it('should return NAMENODE alert', function() {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'NAMENODE',
            display_name: 'Namenode'
          }
        }
      }));
      view.propertyDidChange('alertMessage');
      expect(view.get('alertMessage')).to.be.equal(
        Em.I18n.t('services.reassign.step2.body.namenodeHA').format('Namenode')
      );
    });
  
    it('should return component alert', function() {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'C1',
            display_name: 'c1'
          }
        }
      }));
      view.propertyDidChange('alertMessage');
      expect(view.get('alertMessage')).to.be.equal(
        Em.I18n.t('services.reassign.step2.body').format('c1')
      );
    });
  });
  
});

