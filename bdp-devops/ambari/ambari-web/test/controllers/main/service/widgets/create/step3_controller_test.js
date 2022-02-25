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

App = require('app');

require('controllers/main/service/widgets/create/step3_controller');


describe('App.WidgetWizardStep3Controller', function () {
  var controller = App.WidgetWizardStep3Controller.create({
    content: Em.Object.create()
  });

  App.TestAliases.testAsComputedEqual(controller, 'isEditController', 'content.controllerName', 'widgetEditController');

  App.TestAliases.testAsComputedIfThenElse(controller, 'widgetScope', 'isSharedChecked', 'Cluster', 'User');

  App.TestAliases.testAsComputedOr(controller, 'isSubmitDisabled', ['widgetNameEmpty', 'isNameInvalid', 'isDescriptionInvalid']);

  describe("#validateName", function(){
    var testCases = [
      {
        widgetName: 'abc 123 _ - %',
        result: {
          errorMessage: '',
          isNameInvalid: false
        }
      },
      {
        widgetName: '$#@!',
        result: {
          errorMessage: Em.I18n.t('widget.create.wizard.step3.name.invalidCharacter.msg'),
          isNameInvalid: true
        }
      },
      {
        widgetName: '123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789',
        result: {
          errorMessage: Em.I18n.t('widget.create.wizard.step3.name.invalid.msg'),
          isNameInvalid: true
        },
      },
      {
        widgetName: '',
        result: {
          errorMessage: '',
          isNameInvalid: false
        }
      }
    ];

    testCases.forEach(function(test) {
      it(JSON.stringify(test.widgetName), function () {
        controller.set('widgetName', test.widgetName);
        expect(controller.get('widgetNameErrorMessage')).to.equal(test.result.errorMessage);
        expect(controller.get('isNameInvalid')).to.equal(test.result.isNameInvalid);
      });
    });
  });

  describe("#validateDescription", function(){
    var testCases = [
      {
        widgetDescription: 'abc 123 _ - %',
        result: {
          errorMessage: '',
          isDescriptionInvalid: false
        }
      },
      {
        widgetDescription: '$#@!',
        result: {
          errorMessage: Em.I18n.t('widget.create.wizard.step3.description.invalidCharacter.msg'),
          isDescriptionInvalid: true
        }
      },
      {
        widgetDescription: '123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789',
        result: {
          errorMessage: Em.I18n.t('widget.create.wizard.step3.description.invalid.msg'),
          isDescriptionInvalid: true
        }
      },
      {
        widgetDescription: '',
        result: {
          errorMessage: '',
          isDescriptionInvalid: false
        }
      }
    ];

    testCases.forEach(function(test){
      it(JSON.stringify(test.widgetDescription), function () {
        controller.set('widgetDescription', test.widgetDescription);
        expect(controller.get('descriptionErrorMessage')).to.equal(test.result.errorMessage);
        expect(controller.get('isDescriptionInvalid')).to.equal(test.result.isDescriptionInvalid);
      });
    });
  });

  describe("#initPreviewData()", function () {
    beforeEach(function () {
      sinon.stub(controller, 'addObserver');
      controller.set('content', Em.Object.create({
        widgetProperties: 'widgetProperties',
        widgetValues: 'widgetValues',
        widgetMetrics: 'widgetMetrics',
        widgetAuthor: 'widgetAuthor',
        widgetName: 'widgetName',
        widgetDescription: 'widgetDescription',
        widgetScope: 'CLUSTER',
        controllerName: 'widgetEditController'
      }));
      controller.initPreviewData();
    });
    afterEach(function () {
      controller.addObserver.restore();
    });
    it('checking observes calls', function () {
      controller.get('isSharedCheckboxDisabled') ? expect(controller.addObserver.calledWith('isSharedChecked')).to.be.false:
        expect(controller.addObserver.calledWith('isSharedChecked')).to.be.true;
    });
    it('check widgetProperties`', function () {
      expect(controller.get('widgetProperties')).to.equal('widgetProperties');
    });
    it('check widgetValues', function () {
      expect(controller.get('widgetValues')).to.equal('widgetValues');
    });
    it('check widgetMetrics', function () {
      expect(controller.get('widgetMetrics')).to.equal('widgetMetrics');
    });
    it('check widgetAuthor', function () {
      expect(controller.get('widgetAuthor')).to.equal('widgetAuthor');
    });
    it('check widgetName', function () {
      expect(controller.get('widgetName')).to.equal('widgetName');
    });
    it('check widgetDescription', function () {
      expect(controller.get('widgetDescription')).to.equal('widgetDescription');
    });
    it('check isSharedChecked', function () {
      expect(controller.get('isSharedChecked')).to.be.true;
    });
    it('check isSharedCheckboxDisabled', function () {
      expect(controller.get('isSharedCheckboxDisabled')).to.be.true;
    });
  });

  describe("#showConfirmationOnSharing()", function () {
    beforeEach(function () {
      sinon.spy(App, 'showConfirmationFeedBackPopup');
    });
    afterEach(function () {
      App.showConfirmationFeedBackPopup.restore();
    });
    it("isSharedChecked - false", function () {
      controller.set('isSharedChecked', false);
      controller.showConfirmationOnSharing();
      expect(App.showConfirmationFeedBackPopup.called).to.be.false;
    });
    it("isSharedChecked - true", function () {
      controller.set('isSharedChecked', true);
      var popup = controller.showConfirmationOnSharing();
      expect(App.showConfirmationFeedBackPopup.calledOnce).to.be.true;
      popup.onSecondary();
      expect(controller.get('isSharedChecked')).to.be.false;
      popup.onPrimary();
      expect(controller.get('isSharedChecked')).to.be.true;
    });
  });

  describe("#collectWidgetData()", function () {

    beforeEach(function () {
      controller.setProperties({
        widgetName: 'widgetName',
        content: Em.Object.create({widgetType: 'T1'}),
        widgetDescription: 'widgetDescription',
        widgetScope: 'Cluster',
        widgetAuthor: 'widgetAuthor',
        widgetMetrics: [{data: 'data', name: 'm1'}],
        widgetValues: [{computedValue: 'cv', value: 'v'}],
        widgetProperties: 'widgetProperties'
      });
    });

    it('collected widget data is valid', function () {
      var widgetData = controller.collectWidgetData();
      expect(widgetData).to.eql({
        "WidgetInfo": {
          "widget_name": "widgetName",
          "widget_type": "T1",
          "description": "widgetDescription",
          "scope": "CLUSTER",
          "author": "widgetAuthor",
          "metrics": [
            {"name": "m1" }
          ],
          "values": [
            { "value": "v" }
          ],
          "properties": "widgetProperties"
        }
      });
    });
  });

  describe("#cancel()", function () {
    var mock = {
      cancel: Em.K
    };
    beforeEach(function () {
      sinon.spy(mock, 'cancel');
      sinon.stub(App.router, 'get').returns(mock);
    });
    afterEach(function () {
      App.router.get.restore();
      mock.cancel.restore();
    });
    it('cancel is called', function () {
      controller.cancel();
      expect(mock.cancel.calledOnce).to.be.true;
    });
  });

  describe("#complete()", function () {
    var mock = {
      finishWizard: Em.K
    };
    beforeEach(function () {
      sinon.spy(mock, 'finishWizard');
      sinon.stub(controller, 'collectWidgetData');
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(App.router, 'send');
      controller.complete();
    });
    afterEach(function () {
      App.router.get.restore();
      App.router.send.restore();
      controller.collectWidgetData.restore();
      mock.finishWizard.restore();
    });
    it('widget data is collected', function () {
      expect(controller.collectWidgetData.calledOnce).to.be.true;
    });
    it('user is moved to finish the wizard', function () {
      expect(App.router.send.calledWith('complete')).to.be.true;
    });
    it('finishWizard is called', function () {
      expect(mock.finishWizard.calledOnce).to.be.true;
    });
  });
});
