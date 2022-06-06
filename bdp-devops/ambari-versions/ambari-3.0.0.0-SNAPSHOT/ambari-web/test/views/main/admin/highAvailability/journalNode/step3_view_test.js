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
require('views/main/admin/highAvailability/journalNode/step3_view');

var view;

describe('App.ManageJournalNodeWizardStep3View', function () {

  beforeEach(function() {
    view = App.ManageJournalNodeWizardStep3View.create({
      controller: Em.Object.create({
        pullCheckPointsStatuses: sinon.spy()
      })
    });
  });

  describe("#didInsertElement()", function () {

    it("loadStep is called once", function () {
      view.didInsertElement();
      expect(view.get('controller').pullCheckPointsStatuses.calledOnce).to.be.true;
    });
  });
  
  describe('#step3BodyText', function() {
    beforeEach(function() {
      this.mock = sinon.stub(App.HDFSService, 'find');
    });
    afterEach(function() {
      this.mock.restore();
    });
    
    it('1 namespace', function() {
      this.mock.returns(Em.Object.create({
        masterComponentGroups: [
          {
            name: 'g1'
          }
        ]
      }));
      view.set('controller', {
        content: {
          activeNN: {
            host_name: 'host1'
          },
          hdfsUser: 'hdfs'
        }
      });
      view.propertyDidChange('step3BodyText');
      expect(view.get('step3BodyText')).to.be.equal(
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body').format(
          'host1',
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.safeModeText'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.safeModeCommand').format('hdfs'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.checkPointText'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.checkPointCommand').format('hdfs'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.proceed'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.recentCheckPoint')
        )
      );
    });
  
    it('2 namespaces', function() {
      this.mock.returns(Em.Object.create({
        masterComponentGroups: [
          {
            name: 'g1'
          },
          {
            name: 'g2'
          }
        ]
      }));
      view.set('controller', {
        content: {
          activeNN: {
            host_name: 'host1'
          },
          hdfsUser: 'hdfs'
        }
      });
      view.propertyDidChange('step3BodyText');
      expect(view.get('step3BodyText')).to.be.equal(
        Em.I18n.t('admin.manageJournalNode.wizard.step3.body').format(
          'host1',
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.safeModeText'),
          ['g1', 'g2'].map(function(ns) {
            return Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.safeModeCommand').format('hdfs', ns);
          }).join('<br>'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.checkPointText'),
          ['g1', 'g2'].map(function(ns) {
            return Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.checkPointCommand').format('hdfs', ns);
          }).join('<br>'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.proceed'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.recentCheckPoint')
        )
      );
    });
  });
  
  describe('#nnCheckPointText', function() {
    beforeEach(function() {
      this.mock = sinon.stub(App.HDFSService, 'find');
    });
    afterEach(function() {
      App.HDFSService.find.restore();
    });
    
    it('should be empty when not loaded yet', function() {
      view.set('controller', {
        isHDFSNameSpacesLoaded: false
      });
      view.propertyDidChange('nnCheckPointText');
      expect(view.get('nnCheckPointText')).to.be.empty;
    });
  
    it('1 namespace', function() {
      view.set('controller', {
        isHDFSNameSpacesLoaded: true,
        isNextEnabled: true
      });
      this.mock.returns(Em.Object.create({
        masterComponentGroups: [{}]
      }));
      view.propertyDidChange('nnCheckPointText');
      expect(view.get('nnCheckPointText')).to.be.equal(
        Em.I18n.t('admin.highAvailability.wizard.step4.ckCreated')
      );
    });
  
    it('2 namespaces', function() {
      view.set('controller', {
        isHDFSNameSpacesLoaded: true,
        isNextEnabled: false
      });
      this.mock.returns(Em.Object.create({
        masterComponentGroups: [{}, {}]
      }));
      view.propertyDidChange('nnCheckPointText');
      expect(view.get('nnCheckPointText')).to.be.equal(
        Em.I18n.t('admin.manageJournalNode.wizard.step3.checkPointsNotCreated')
      );
    });
  });
  
  describe('#errorText', function() {
    beforeEach(function() {
      this.mock = sinon.stub(App.HDFSService, 'find');
    });
    afterEach(function() {
      App.HDFSService.find.restore();
    });
    
    it('should be empty when not loaded yet', function() {
      view.set('controller', {
        isHDFSNameSpacesLoaded: false
      });
      view.propertyDidChange('errorText');
      expect(view.get('errorText')).to.be.empty;
    });
    
    it('1 namespace', function() {
      view.set('controller', {
        isHDFSNameSpacesLoaded: true,
        isNameNodeStarted: false
      });
      this.mock.returns(Em.Object.create({
        masterComponentGroups: [{}]
      }));
      view.propertyDidChange('errorText');
      expect(view.get('errorText')).to.be.equal(
        Em.I18n.t('admin.highAvailability.wizard.step4.error.nameNode')
      );
    });
    
    it('2 namespaces', function() {
      view.set('controller', {
        isHDFSNameSpacesLoaded: true,
        isActiveNameNodesStarted: false
      });
      this.mock.returns(Em.Object.create({
        masterComponentGroups: [{}, {}]
      }));
      view.propertyDidChange('errorText');
      expect(view.get('errorText')).to.be.equal(
        Em.I18n.t('admin.manageJournalNode.wizard.step3.error.multipleNameSpaces.nameNodes')
      );
    });
  });
});
