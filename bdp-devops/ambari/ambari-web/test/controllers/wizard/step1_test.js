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

var wizardStep1Controller;

var stacks = [
  App.Stack.createRecord({
    "id": "HDP-2.4",
    "stackName": "HDP",
    "stackVersion": "2.4"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.5-2.5.0.0",
    "stackName": "HDP",
    "stackVersion": "2.5"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.5",
    "stackName": "HDP",
    "stackVersion": "2.5"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.3.ECS",
    "stackName": "HDP",
    "stackVersion": "2.3.ECS"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.3",
    "stackName": "HDP",
    "stackVersion": "2.3"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.2",
    "stackName": "HDP",
    "stackVersion": "2.2"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.4-2.4.1.1-12345",
    "stackName": "HDP",
    "stackVersion": "2.4"
  })
];

function getController() {
  return App.WizardStep1Controller.create({content: Em.Object.create({stacks: stacks}), onNetworkIssuesExist: Em.K});
}

describe('App.WizardStep1Controller', function () {

  beforeEach(function() {
    wizardStep1Controller = getController();
  });

  App.TestAliases.testAsComputedFindBy(getController(), 'selectedStack', 'content.stacks', 'isSelected', true);

  App.TestAliases.testAsComputedFindBy(getController(), 'selectedStackType', 'availableStackTypes', 'isSelected', true);

  App.TestAliases.testAsComputedFilterBy(getController(), 'servicesForSelectedStack', 'selectedStack.stackServices', 'isHidden', false);

  App.TestAliases.testAsComputedEveryBy(getController(), 'networkIssuesExist', 'content.stacks', 'stackDefault', true);

  App.TestAliases.testAsComputedEqual(getController(), 'isLoadingComplete', 'wizardController.loadStacksRequestsCounter', 0);

  describe('#usePublicRepo', function () {

    beforeEach(function () {
      wizardStep1Controller.get('content.stacks').findProperty('id', 'HDP-2.5-2.5.0.0').setProperties({
        isSelected: true,
        useRedhatSatellite: true,
        usePublicRepo: false,
        useLocalRepo: true,
      });
      wizardStep1Controller.usePublicRepo();
    });

    it('correct stack is selected', function () {
      expect(wizardStep1Controller.get('selectedStack.id')).to.be.equal('HDP-2.5-2.5.0.0');
    });

    it('`useRedhatSatellite` is set `false`', function () {
      expect(wizardStep1Controller.get('selectedStack.useRedhatSatellite')).to.be.false;
    });

    it('`usePublicRepo` is set `true`', function () {
      expect(wizardStep1Controller.get('selectedStack.usePublicRepo')).to.be.true;
    });

    it('`useLocalRepo` is set `false`', function () {
      expect(wizardStep1Controller.get('selectedStack.useLocalRepo')).to.be.false;
    });

  });

  describe('#useLocalRepo', function () {

    beforeEach(function () {
      wizardStep1Controller.get('content.stacks').findProperty('id', 'HDP-2.5-2.5.0.0').setProperties({
        isSelected: true,
        usePublicRepo: true,
        useLocalRepo: false,
      });
      wizardStep1Controller.useLocalRepo();
    });

    it('correct stack is selected', function () {
      expect(wizardStep1Controller.get('selectedStack.id')).to.be.equal('HDP-2.5-2.5.0.0');
    });

    it('`usePublicRepo` is set `false`', function () {
      expect(wizardStep1Controller.get('selectedStack.usePublicRepo')).to.be.false;
    });

    it('`useLocalRepo` is set `true`', function () {
      expect(wizardStep1Controller.get('selectedStack.useLocalRepo')).to.be.true;
    });

  });

  describe('#selectStackBy', function () {

    it('select by `id`', function () {
      wizardStep1Controller.selectStackBy('id', 'HDP-2.5-2.5.0.0');
      expect(wizardStep1Controller.get('selectedStack.id')).to.be.equal('HDP-2.5-2.5.0.0');
      expect(wizardStep1Controller.get('content.stacks').filterProperty('isSelected')).to.have.property('length').equal(1);
    });

    it('select by `stackNameVersion`', function () {
      wizardStep1Controller.selectStackBy('stackNameVersion', 'HDP-2.5');
      expect(wizardStep1Controller.get('selectedStack.id')).to.be.equal('HDP-2.5-2.5.0.0'); // `HDP-2.5-2.5.0.0`-id is before `HDP-2.5`-id
      expect(wizardStep1Controller.get('content.stacks').filterProperty('isSelected')).to.have.property('length').equal(1);
    });

  });

  describe('#availableStackTypes', function () {

    it('stack types are sorted desc', function () {
      expect(wizardStep1Controller.get('availableStackTypes').mapProperty('stackName')).to.be.eql(['HDP-2.5', 'HDP-2.4', 'HDP-2.3.ECS', 'HDP-2.3', 'HDP-2.2']);
    });

  });

  describe('#readInfoIsNotProvided', function () {

    Em.A([
      {
        options: {
          uploadFile: {isSelected: false},
          enterUrl: {isSelected: false}
        },
        m: 'url and file are not selected',
        e: false
      },
      {
        options: {
          uploadFile: {isSelected: false},
          enterUrl: {isSelected: true, url: ''}
        },
        m: 'url is selected but not provided',
        e: true
      },
      {
        options: {
          uploadFile: {isSelected: false},
          enterUrl: {isSelected: true, url: ' url'}
        },
        m: 'url is selected and provided',
        e: false
      },
      {
        options: {
          uploadFile: {isSelected: true, file: ''},
          enterUrl: {isSelected: false}
        },
        m: 'file is selected but not provided',
        e: true
      },
      {
        options: {
          uploadFile: {isSelected: true, file: 'path'},
          enterUrl: {isSelected: false}
        },
        m: 'file is selected and provided',
        e: false
      }
    ]).forEach(function (test) {

      it(test.m, function () {
        wizardStep1Controller.set('optionsToSelect.useLocalRepo', test.options);
        expect(wizardStep1Controller.get('readInfoIsNotProvided')).to.be.equal(test.e);
      });

    });

  });

  describe('#uploadVdf', function () {

    function getModal() {
      var controller = getController();
      controller.set('optionsToSelect', Em.Object.create({
        useLocalRepo: {
          enterUrl: {isSelected: true, url: 'apache.org'},
          uploadFile: {isSelected: false, file: 'some_file'}
        }
      }));
      return controller.uploadVdf();
    }

    beforeEach(function () {
      this.modal = getModal();
    });

    describe('#restoreUploadOptions', function () {

      beforeEach(function () {
        wizardStep1Controller.set('optionsToSelect.useLocalRepo', {
          enterUrl: {isSelected: true, url: 'apache.org'},
          uploadFile: {isSelected: false, file: 'some_file'}
        });
        this.modal.restoreUploadOptions();
      });

      it('`enterUrl.isSelected`', function () {
        expect(this.modal.get('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected')).to.be.false;
      });

      it('`enterUrl.url`', function () {
        expect(this.modal.get('controller.optionsToSelect.useLocalRepo.enterUrl.url')).to.be.equal('');
      });

      it('`uploadFile.isSelected`', function () {
        expect(this.modal.get('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected')).to.be.true;
      });

      it('`uploadFile.file`', function () {
        expect(this.modal.get('controller.optionsToSelect.useLocalRepo.uploadFile.file')).to.be.equal('');
      });

    });

    describe('#bodyClass', function () {

      beforeEach(function () {
        this.body = this.modal.get('bodyClass').create();
      });

      describe('#uploadFileView', function () {

        beforeEach(function() {
          this.fileView = this.body.get('uploadFileView').create();
        });

        describe('#click', function () {

          beforeEach(function () {
            this.fileView.set('controller', getController());
            this.fileView.set('controller.optionsToSelect', {
              useLocalRepo: {
                enterUrl: {isSelected: true, hasError: true},
                uploadFile: {isSelected: false, hasError: true}
              }
            });
            this.fileView.click();
          });

          it('`enterUrl.isSelected`', function () {
            expect(this.fileView.get('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected')).to.be.false;
          });

          it('`enterUrl.hasError`', function () {
            expect(this.fileView.get('controller.optionsToSelect.useLocalRepo.enterUrl.hasError')).to.be.false;
          });

          it('`uploadFile.isSelected`', function () {
            expect(this.fileView.get('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected')).to.be.true;
          });

          it('`uploadFile.hasError`', function () {
            expect(this.fileView.get('controller.optionsToSelect.useLocalRepo.uploadFile.hasError')).to.be.false;
          });

        });

      });

      describe('#enterUrlView', function () {

        beforeEach(function() {
          this.fileView = this.body.get('enterUrlView').create();
        });

        describe('#click', function () {

          beforeEach(function () {
            this.fileView.set('controller', getController());
            this.fileView.set('controller.optionsToSelect', {
              useLocalRepo: {
                enterUrl: {isSelected: false, hasError: true},
                uploadFile: {isSelected: false, hasError: true}
              }
            });
            this.fileView.click();
          });

          it('`enterUrl.isSelected`', function () {
            expect(this.fileView.get('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected')).to.be.true;
          });

          it('`enterUrl.hasError`', function () {
            expect(this.fileView.get('controller.optionsToSelect.useLocalRepo.enterUrl.hasError')).to.be.false;
          });

          it('`uploadFile.isSelected`', function () {
            expect(this.fileView.get('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected')).to.be.false;
          });

          it('`uploadFile.hasError`', function () {
            expect(this.fileView.get('controller.optionsToSelect.useLocalRepo.uploadFile.hasError')).to.be.false;
          });

        });

      });

    });

  })

  describe('#removeOS', function() {

    beforeEach(function () {
      wizardStep1Controller.set('selectedStack', {useRedhatSatellite: null});
    });

    [
      {
        useRedhatSatellite: false,
        e: false,
      },
      {
        useRedhatSatellite: true,
        e: true
      }
    ].forEach(function (test) {
      it('useRedhatSatellite is ' + JSON.stringify(test.useRedhatSatellite), function () {
        wizardStep1Controller.set('selectedStack.useRedhatSatellite', test.useRedhatSatellite);
        var os = {isSelected: true};
        wizardStep1Controller.removeOS({context: os})
        expect(Ember.get(os, 'isSelected')).to.be.equal(test.e);
      });
    });

  });

  describe('#addOS', function() {

    it('should set `isSelected` to true', function () {
      var os = {isSelected: false};
      wizardStep1Controller.addOS({context: os})
      expect(Ember.get(os, 'isSelected')).to.be.true;
    });

  });

  describe('#onNetworkIssuesExist', function () {

    beforeEach(function () {
      this.controller = App.WizardStep1Controller.create({content: Em.Object.create({stacks: stacks})});
      this.controller.get('content.stacks').setEach('usePublicRepo', true);
      this.controller.get('content.stacks').setEach('useLocalRepo', false);
      this.controller.reopen({networkIssuesExist: true});
      this.controller.onNetworkIssuesExist();
    });

    it('each stack has `usePublicRepo` false', function() {
      expect(this.controller.get('content.stacks').everyProperty('usePublicRepo', false)).to.be.true;
    });

    it('each stack has `useLocalRepo` true', function() {
      expect(this.controller.get('content.stacks').everyProperty('useLocalRepo', true)).to.be.true;
    });

  });

});
