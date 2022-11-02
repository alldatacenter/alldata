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
require('views/main/service/reassign/step1_view');
var stringUtils = require('utils/string_utils');

var view;

function getView() {
  return App.ReassignMasterWizardStep1View.create();
}

describe('App.ReassignMasterWizardStep1View', function () {
  
  beforeEach(function() {
    view = getView();
  });
  
  describe('#message', function() {
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'S1'}, {serviceName: 'HDFS'}]);
    });
    afterEach(function() {
      App.Service.find.restore();
    });
    
    it('should return message with all installed services', function() {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'C1',
            display_name: 'c1'
          },
          componentsToStopAllServices: ['C1']
        }
      }));
      view.propertyDidChange('message');
      expect(view.get('message')).to.be.eql([
        Em.I18n.t('services.reassign.step1.message1').format('c1'),
        Em.I18n.t('services.reassign.step1.message3').format(stringUtils.getFormattedStringFromArray(['S1', 'HDFS']), 'c1')
      ]);
    });
  
    it('should return message with all installed services, has manual steps', function() {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'C1',
            display_name: 'c1'
          },
          componentsToStopAllServices: ['C1'],
          hasManualSteps: true
        }
      }));
      view.propertyDidChange('message');
      expect(view.get('message')).to.be.eql([
        Em.I18n.t('services.reassign.step1.message1').format('c1'),
        Em.I18n.t('services.reassign.step1.message2').format('c1'),
        Em.I18n.t('services.reassign.step1.message3').format(stringUtils.getFormattedStringFromArray(['S1', 'HDFS']), 'c1')
      ]);
    });
  
    it('should return message with non-HDFS services', function() {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'C1',
            display_name: 'c1'
          },
          componentsToStopAllServices: []
        },
        target: {
          reassignMasterController: {
            relatedServicesMap: {}
          }
        }
      }));
      view.propertyDidChange('message');
      expect(view.get('message')).to.be.eql([
        Em.I18n.t('services.reassign.step1.message1').format('c1'),
        Em.I18n.t('services.reassign.step1.message3').format(stringUtils.getFormattedStringFromArray(['S1']), 'c1')
      ]);
    });
  
    it('should return message with related services', function() {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'C1',
            display_name: 'c1'
          },
          componentsToStopAllServices: []
        },
        target: {
          reassignMasterController: {
            relatedServicesMap: {
              'C1': ['S2', 'S1']
            }
          }
        }
      }));
      view.propertyDidChange('message');
      expect(view.get('message')).to.be.eql([
        Em.I18n.t('services.reassign.step1.message1').format('c1'),
        Em.I18n.t('services.reassign.step1.message3').format(stringUtils.getFormattedStringFromArray(['S1']), 'c1')
      ]);
    });
    
  });
  
});

