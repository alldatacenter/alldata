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

var view;

function getView() {
  return App.ConfigWidgetView.create({
    initPopover: Em.K,
    movePopover: Em.K,
    config: Em.Object.create({
      isOriginalSCP: false,
      isPropertyOverridable: false,
      cantBeUndone: false,
      isNotDefaultValue: false
    })
  });
}

describe('App.ConfigWidgetView', function () {

  beforeEach(function () {
    view = getView();
  });

  App.TestAliases.testAsComputedAnd(getView(), 'showPencil', ['supportSwitchToTextBox', '!disabled']);

  App.TestAliases.testAsComputedOr(getView(), 'doNotShowWidget', ['isPropertyUndefined', 'config.showAsTextBox']);

  App.TestAliases.testAsComputedEqual(getView(), 'isPropertyUndefined', 'config.value', 'Undefined');

  describe('#undoAllowed', function () {

    Em.A([
      {
        cfg: {
          cantBeUndone: false,
          isNotDefaultValue: false
        },
        view: {
          disabled: false,
          isOriginalSCP: false
        },
        e: false
      },
      {
        cfg: {
          cantBeUndone: true,
          isNotDefaultValue: false
        },
        view: {
          disabled: false,
          isOriginalSCP: false
        },
        e: false
      },
      {
        cfg: {
          cantBeUndone: false,
          isNotDefaultValue: true
        },
        view: {
          disabled: false,
          isOriginalSCP: true
        },
        e: true
      },
      {
        cfg: {
          cantBeUndone: true,
          isNotDefaultValue: true
        },
        view: {
          disabled: true,
          isOriginalSCP: false
        },
        e: false
      }
    ]).forEach(function (test, index) {
        it('test #' + index, function () {
          view.get('config').setProperties(test.cfg);
          view.setProperties(test.view);
          expect(view.get('undoAllowed')).to.equal(test.e);
        });
      });

  });

  describe('#overrideAllowed', function () {

    Em.A([
        {
          cfg: {
            isOriginalSCP: false,
            isPropertyOverridable: false,
            isComparison: false
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: true,
            isPropertyOverridable: false,
            isComparison: false
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: false,
            isPropertyOverridable: true,
            isComparison: false
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: true,
            isPropertyOverridable: true,
            isComparison: false
          },
          e: true
        },
        {
          cfg: {
            isOriginalSCP: false,
            isPropertyOverridable: false,
            isComparison: true
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: true,
            isPropertyOverridable: false,
            isComparison: true
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: false,
            isPropertyOverridable: true,
            isComparison: true
          },
          e: false
        },
        {
          cfg: {
            isOriginalSCP: true,
            isPropertyOverridable: true,
            isComparison: true
          },
          e: false
        }
      ]).forEach(function (test, index) {
        it('test #' + index, function () {
          view.get('config').setProperties(test.cfg);
          expect(view.get('overrideAllowed')).to.equal(test.e);
        });
      });

  });

  describe('#restoreDependentConfigs', function() {
    beforeEach(function() {
      view = App.ConfigWidgetView.create({
        controller: Em.Object.extend(App.EnhancedConfigsMixin, {
        }).create({
          updateDependentConfigs: function() {}
        }),
        config: Em.Object.create({ name: 'config1'})
      });
    });

    var tests = [
      {
        dependentConfigs: [
          {name: 'dependent1', parentConfigs: ['config1']},
          {name: 'dependent2', parentConfigs: ['config2']},
          {name: 'dependent3', parentConfigs: ['config1']}
        ],
        e: ['dependent2'],
        m: 'when dependent configs has one parent they should be removed'
      },
      {
        dependentConfigs: [
          {name: 'dependent1', parentConfigs: ['config1', 'config2']},
          {name: 'dependent2', parentConfigs: ['config2']},
          {name: 'dependent3', parentConfigs: ['config1']}
        ],
        e: ['dependent1', 'dependent2'],
        m: 'when dependent configs has multiple parents they should not be removed'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('controller.recommendations', test.dependentConfigs);
        view.restoreDependentConfigs(view.get('config'));
        expect(view.get('controller.recommendations').mapProperty('name')).to.be.eql(test.e);
      });
    });

    describe('when dependent configs has multiple parents appropriate parent config should be removed', function() {

      beforeEach(function () {
        view.set('controller.recommendations', [
          {name: 'dependent1', parentConfigs: ['config1', 'config2']},
          {name: 'dependent2', parentConfigs: ['config2', 'config1']},
          {name: 'dependent3', parentConfigs: ['config1']}
        ]);
        view.restoreDependentConfigs(view.get('config'));
      });

      it('2 recommendations', function () {
        expect(view.get('controller.recommendations.length')).to.be.equal(2);
      });

      it('dependent1 parent is ["config2"]', function () {
        expect(view.get('controller.recommendations').findProperty('name', 'dependent1').parentConfigs.toArray()).to.be.eql(["config2"]);
      });
      it('dependent2 parent is ["config2"]', function () {
        expect(view.get('controller.recommendations').findProperty('name', 'dependent2').parentConfigs.toArray()).to.be.eql(["config2"]);
      });

    });

    describe('dependent config value should be set with inital or saved when it has one parent', function() {
      var ctrl;

      beforeEach(function () {
        ctrl = view.get('controller');
        ctrl.set('stepConfigs', [
          Em.Object.create({
            configs: Em.A([
              Em.Object.create({ name: 'dependent3', savedValue: '1', value: 2, filename: 'some-file.xml' }),
              Em.Object.create({ name: 'dependent2', savedValue: '4', value: '10', filename: 'some-file.xml' })
            ])
          })
        ]);
        view.set('controller.recommendations', [
          {propertyName: 'dependent1', parentConfigs: ['config1', 'config2'], fileName: 'some-file' },
          {propertyName: 'dependent2', parentConfigs: ['config2', 'config1'], fileName: 'some-file'},
          {propertyName: 'dependent3', parentConfigs: ['config1'], fileName: 'some-file' }
        ]);
        view.restoreDependentConfigs(view.get('config'));
      });

      it('dependent3 value is `1`', function () {
        expect(App.config.findConfigProperty(ctrl.get('stepConfigs'), 'dependent3', 'some-file.xml').get('value')).to.be.equal('1');
      });

      it('dependent2 value is `10`', function () {
        // config with multi dependency should not be updated
        expect(App.config.findConfigProperty(ctrl.get('stepConfigs'), 'dependent2', 'some-file.xml').get('value')).to.be.equal('10');
      });

    });

  });

  describe('#isValueCompatibleWithWidget()', function() {
    it('pass validation', function() {
      view.set('config.isValid', true);
      expect(view.isValueCompatibleWithWidget()).to.be.true;
    });

    it('fail validation', function() {
      view.set('config.isValid', false);
      view.set('supportSwitchToTextBox', true);
      expect(view.isValueCompatibleWithWidget()).to.be.false;
    });
  });

  describe('#setRecommendedValue', function () {

    beforeEach(function () {
      sinon.stub(view, 'sendRequestRorDependentConfigs', function () {
        return $.Deferred().resolve().promise();
      });
      sinon.stub(view, 'restoreDependentConfigs', Em.K);
      view.set('config', Em.Object.create({
        value: 1,
        recommendedValue: 1,
        savedValue: 1
      }));
    });

    afterEach(function () {
      view.sendRequestRorDependentConfigs.restore();
      view.restoreDependentConfigs.restore();
    });

    it('should call restoreDependentConfigs if config.value is equal to config.savedValue', function () {
      view.setRecommendedValue();
      expect(view.restoreDependentConfigs.calledOnce).to.be.true;
    });

    it('should not call restoreDependentConfigs if config.value is not equal to config.savedValue', function () {
      view.set('config.savedValue', 2);
      view.setRecommendedValue();
      expect(view.restoreDependentConfigs.called).to.be.false;
    });

  });

  describe('#showFinalConfig', function () {

    [
      {
        config: {
          isFinal: true,
          isNotEditable: true
        },
        e: true
      },
      {
        config: {
          isFinal: true,
          isNotEditable: false
        },
        e: true
      },
      {
        config: {
          isFinal: false,
          isNotEditable: true
        },
        e: false
      },
      {
        config: {
          isFinal: false,
          isNotEditable: false
        },
        e: true
      }
    ].forEach(function (test) {

      it(JSON.stringify(test.config), function () {
        view.set('config', Em.Object.create(test.config));
        expect(view.get('showFinalConfig')).to.be.equal(test.e);
      });

    })

  });

  describe('#toggleFinalFlag', function () {

    [
      {isNotEditable: true, isFinal: false},
      {isNotEditable: false, isFinal: true}
    ].forEach(function (test) {
      it('config.isNotEditable ' + test.isNotEditable, function () {
        var config = Em.Object.create({isNotEditable: test.isNotEditable, isFinal: false});
        view.toggleFinalFlag({context: config});
        expect(config.get('isFinal')).to.be.equal(test.isFinal);
      });
    });

  });

  describe('#issueView', function () {

    beforeEach(function () {
      this.issueView = getView().get('issueView').create({config: Em.Object.create()});
      sinon.stub(App, 'tooltip', Em.K);
    });

    afterEach(function () {
      App.tooltip.restore();
    });

    describe('#didInsertElement', function () {

      beforeEach(function () {
        this.issueView.errorLevelObserver = Em.K;
        sinon.spy(this.issueView, 'addObserver');
      });

      afterEach(function () {
        this.issueView.addObserver.restore();
      });

      [
        'issuedConfig.warnMessage',
        'issuedConfig.errorMessage',
        'parentView.isPropertyUndefined'
      ].forEach(function (field) {
        it('add observer for ' + field, function () {
          this.issueView.didInsertElement();
          expect(this.issueView.addObserver.calledWith(field, this.issueView, this.issueView.errorLevelObserver)).to.be.true;
        });
      });

    });

    describe('#willDestroyElement', function () {

      beforeEach(function () {
        this.issueView.errorLevelObserver = Em.K;
        sinon.spy(this.issueView, 'removeObserver');
      });

      afterEach(function () {
        this.issueView.removeObserver.restore();
      });

      [
        'issuedConfig.warnMessage',
        'issuedConfig.errorMessage',
        'parentView.isPropertyUndefined'
      ].forEach(function (field) {
        it('remove observer for ' + field, function () {
          this.issueView.willDestroyElement();
          expect(this.issueView.removeObserver.calledWith(field, this.issueView, this.issueView.errorLevelObserver)).to.be.true;
        });
      });

    });

    describe('#errorLevelObserver', function () {

      beforeEach(function () {

        this.issueView.set('parentView', Em.Object.create());

      });

      [
        {
          issuedConfig: Em.Object.create({
            errorMessage: '123',
            warnMessage: ''
          }),
          isPropertyUndefined: true,
          e: {
            configLabelClass: '',
            issueIconClass: 'hide',
            issueMessage: false
          }
        },
        {
          issuedConfig: Em.Object.create({
            errorMessage: '123',
            warnMessage: ''
          }),
          isPropertyUndefined: false,
          e: {
            configLabelClass: 'text-danger',
            issueIconClass: '',
            issueMessage: '123'
          }
        },
        {
          issuedConfig: Em.Object.create({
            errorMessage: '',
            warnMessage: '321'
          }),
          isPropertyUndefined: true,
          e: {
            configLabelClass: '',
            issueIconClass: 'hide',
            issueMessage: false
          }
        },
        {
          issuedConfig: Em.Object.create({
            errorMessage: '',
            warnMessage: '321'
          }),
          isPropertyUndefined: false,
          e: {
            configLabelClass: 'text-warning',
            issueIconClass: 'warning',
            issueMessage: '321'
          }
        },
        {
          issuedConfig: Em.Object.create({
            errorMessage: '',
            warnMessage: ''
          }),
          isPropertyUndefined: true,
          e: {
            configLabelClass: '',
            issueIconClass: 'hide',
            issueMessage: false
          }
        },
        {
          issuedConfig: Em.Object.create({
            errorMessage: '',
            warnMessage: ''
          }),
          isPropertyUndefined: false,
          e: {
            configLabelClass: '',
            issueIconClass: 'hide',
            issueMessage: false
          }
        },
      ].forEach(function (test, index) {
        describe('case #' + (index + 1), function () {

          beforeEach(function () {
            this.issueView.reopen({issuedConfig: test.issuedConfig, parentView: Em.Object.create()});
            this.issueView.set('parentView.isPropertyUndefined', test.isPropertyUndefined);
            this.issueView.errorLevelObserver();
          });

          it('`parentView.configLabelClass`', function () {
            expect(this.issueView.get('parentView.configLabelClass')).to.be.equal(test.e.configLabelClass);
          });

          it('`issueIconClass`', function () {
            expect(this.issueView.get('issueIconClass')).to.be.equal(test.e.issueIconClass);
          });

          it('`issueMessage`', function () {
            expect(this.issueView.get('issueMessage')).to.be.equal(test.e.issueMessage);
          });

          it('`parentView.issueMessage`', function () {
            expect(this.issueView.get('parentView.issueMessage')).to.be.equal(test.e.issueMessage);
          });

        });
      });

    });

  });

  describe('#updateWarningsForCompatibilityWithWidget', function () {

    [
      {
        message: '',
        configLabelClass: ''
      },
      {
        message: 'not empty message',
        configLabelClass: 'text-warning'
      }
    ].forEach(function (test) {
      describe('message - ' + JSON.stringify(test.message), function () {

        beforeEach(function () {
          view.set('config', Em.Object.create());
          view.updateWarningsForCompatibilityWithWidget(test.message);
        });

        it('`warnMessage`', function () {
          expect(view.get('warnMessage')).to.be.equal(test.message);
        });

        it('`config.warnMessage`', function () {
          expect(view.get('config.warnMessage')).to.be.equal(test.message);
        });

        it('`issueMessage`', function () {
          expect(view.get('issueMessage')).to.be.equal(test.message);
        });

        it('`configLabelClass`', function () {
          expect(view.get('configLabelClass')).to.be.equal(test.configLabelClass);
        });

      });
    })

  });

  describe('#widgetToTextBox', function () {

    it('should set `config.showAsTextBox` true', function () {
      Em.setFullPath(view, 'config.showAsTextBox', false);
      view.widgetToTextBox();
      expect(view.get('config.showAsTextBox')).to.be.true;
    });

  });

});
