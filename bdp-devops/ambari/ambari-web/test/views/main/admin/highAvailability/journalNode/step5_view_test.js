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
require('views/main/admin/highAvailability/journalNode/step5_view');

var view;

describe('App.ManageJournalNodeWizardStep5View', function () {

  beforeEach(function() {
    view = App.ManageJournalNodeWizardStep5View.create();
  });
  
  describe('#step5BodyText', function() {
    beforeEach(function() {
      sinon.stub(App.HDFSService, 'find').returns(Em.Object.create({
        masterComponentGroups: [
          {
            name: 'g1'
          }
        ]
      }));
    });
    afterEach(function() {
      App.HDFSService.find.restore();
    });
    
    it('should return step5 text', function() {
      view.set('controller', {
        content: {
          masterComponentHosts: [
            {
              component: 'JOURNALNODE',
              isInstalled: true,
              hostName: 'host1'
            }
          ],
          serviceConfigProperties: {
            items: [{
              type: 'hdfs-site',
              properties: {
                'dfs.journalnode.edits.dir': 'dir1'
              }
            }]
          }
        }
      });
      view.propertyDidChange('step5BodyText');
      expect(view.get('step5BodyText')).to.be.equal(
        Em.I18n.t('admin.manageJournalNode.wizard.step5.body').format('host1', '<b>dir1</b>')
      );
    });
  });
});
