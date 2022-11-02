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
var numberUtils = require("utils/number_utils");
var misc = require('utils/misc');


describe('App.WidgetWizardExpressionView', function () {
  var view;

  beforeEach(function() {
    view = App.WidgetWizardExpressionView.create({
      expression: {
        data: []
      },
      controller: Em.Object.create({
        updateExpressions: Em.K
      })
    });
    view.removeObserver('expression.data.length', view, 'validate');
  });

  describe("#validate()", function() {
    var testCases = [
      {
        data: [],
        result: false
      },
      {
        data: [
          {isMetric: true, name:'1'}
        ],
        result: false
      },
      {
        data: [
          {isMetric: true, name:'1'},
          {name: '+'},
          {isMetric: true, name:'1'}
        ],
        result: false
      },
      {
        data: [
          {name: '('},
          {isMetric: true, name:'1'},
          {name: '-'},
          {isMetric: true, name:'1'},
          {name: ')'}
        ],
        result: false
      },
      {
        data: [
          {name: '('},
          {isMetric: true, name:'1'},
          {name: '-'},
          {isMetric: true, name:'1'},
          {name: ')'},
          {name: '*'},
          {isMetric: true, name:'1'}
        ],
        result: false
      },
      {
        data: [
          {name: '('},
          {name: '('},
          {isMetric: true, name:'1'},
          {name: '-'},
          {isMetric: true, name:'1'},
          {name: ')'},
          {name: '*'},
          {isMetric: true, name:'1'},
          {name: ')'}
        ],
        result: false
      },
      {
        data: [
          {name: '-'}
        ],
        result: true
      },
      {
        data: [
          {name: '-'},
          {isMetric: true, name:'1'}
        ],
        result: true
      },
      {
        data: [
          {isMetric: true, name:'1'},
          {name: '+'}
        ],
        result: true
      },
      {
        data: [
          {name: '*'},
          {isMetric: true, name:'1'},
          {name: '+'}
        ],
        result: true
      },
      {
        data: [
          {isMetric: true, name:'1'},
          {name: '('}
        ],
        result: true
      },
      {
        data: [
          {name: ')'},
          {isMetric: true, name:'1'}
        ],
        result: true
      },
      {
        data: [
          {name: '('}
        ],
        result: true
      },
      {
        data: [
          {name: '('},
          {isMetric: true, name:'1'}
        ],
        result: true
      },
      {
        data: [
          {isMetric: true, name:'1'},
          {name: '('}
        ],
        result: true
      },
      {
        data: [
          {name: '('},
          {isMetric: true, name:'1'},
          {name: '+'},
          {isMetric: true, name:'1'}
        ],
        result: true
      },
      {
        data: [
          {isMetric: true, name:'1'},
          {name: '+'},
          {isMetric: true, name:'1'},
          {name: ')'}
        ],
        result: true
      },
      {
        data: [
          {name: '('},
          {name: '('},
          {isMetric: true, name:'1'},
          {name: '+'},
          {isMetric: true, name:'1'},
          {name: ')'}
        ],
        result: true
      }
    ];
    testCases.forEach(function (test) {
      it(test.data.mapProperty('name').join("") + " - isInvalid = " + test.result, function () {
        view.set('expression.data', test.data);
        view.validate();
        expect(view.get('isInvalid')).to.equal(test.result);
      });
    }, this);
  });

  describe("#isNumberValueInvalid", function() {

    beforeEach(function() {
      sinon.stub(numberUtils, 'isPositiveNumber').returns(true)
    });
    afterEach(function() {
      numberUtils.isPositiveNumber.restore();
    });

    it("numberValue is empty", function() {
      view.set('numberValue', '');
      view.propertyDidChange('isNumberValueInvalid');
      expect(view.get('isNumberValueInvalid')).to.be.true;
    });

    it("numberValue is ' '", function() {
      view.set('numberValue', ' ');
      view.propertyDidChange('isNumberValueInvalid');
      expect(view.get('isNumberValueInvalid')).to.be.true;
    });

    it("numberValue is '2'", function() {
      view.set('numberValue', '2');
      view.propertyDidChange('isNumberValueInvalid');
      expect(view.get('isNumberValueInvalid')).to.be.false;
    });
  });

  describe("#addOperator()", function() {

    it("add first operator", function() {
      var event = {context: 'o1'};
      view.set('expression', Em.Object.create({
        data: []
      }));
      view.addOperator(event);
      expect(view.get('expression.data').mapProperty('id')).to.eql([1]);
      expect(view.get('expression.data').mapProperty('name')).to.eql(['o1']);
    });

    it("add second operator", function() {
      var event = {context: 'o2'};
      view.set('expression', Em.Object.create({
        data: [{id: 1, name: 'o1'}]
      }));
      view.addOperator(event);
      expect(view.get('expression.data').mapProperty('id')).to.eql([1, 2]);
      expect(view.get('expression.data').mapProperty('name')).to.eql(['o1', 'o2']);
    });
  });

  describe("#addNumber()", function() {

    it("add first number", function() {
      view.set('expression', Em.Object.create({
        data: []
      }));
      view.set('numberValue', '1');
      view.addNumber();
      expect(view.get('expression.data').mapProperty('id')).to.eql([1]);
      expect(view.get('expression.data').mapProperty('name')).to.eql(['1']);
      expect(view.get('numberValue')).to.be.empty;
    });

    it("add second number", function() {
      view.set('expression', Em.Object.create({
        data: [{id: 1, name: '1'}]
      }));
      view.set('numberValue', '2');
      view.addNumber();
      expect(view.get('expression.data').mapProperty('id')).to.eql([1, 2]);
      expect(view.get('expression.data').mapProperty('name')).to.eql(['1', '2']);
      expect(view.get('numberValue')).to.be.empty;
    });
  });

  describe("#redrawField()", function() {

    beforeEach(function() {
      sinon.stub(misc, 'sortByOrder').returns([{}]);
    });
    afterEach(function() {
      misc.sortByOrder.restore();
    });

    it("sortByOrder should be called", function() {
      view.redrawField();
      expect(misc.sortByOrder.calledOnce).to.be.true;
      expect(view.get('expression.data')).to.be.eql([{}]);
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(view, 'propertyDidChange');
      sinon.stub(Em.run, 'next');
    });
    afterEach(function() {
      view.propertyDidChange.restore();
      Em.run.next.restore();
    });

    it("Em.run.next should be called", function() {
      view.didInsertElement();
      expect(view.propertyDidChange.calledOnce).to.be.true;
      expect(Em.run.next.calledOnce).to.be.true;
    });
  });

  describe("#removeElement()", function() {

    it("object should be removed", function() {
      var event = {context: 'el1'};
      view.set('expression.data', [event.context]);
      view.removeElement(event);
      expect(view.get('expression.data')).to.be.empty;
    });
  });
});

describe("App.AddNumberExpressionView", function() {

  var view;

  beforeEach(function() {
    view = App.AddNumberExpressionView.create();
  });

  describe("#isInvalid", function() {

    beforeEach(function() {
      sinon.stub(numberUtils, 'isPositiveNumber').returns(true)
    });
    afterEach(function() {
      numberUtils.isPositiveNumber.restore();
    });

    it("value is empty", function() {
      view.set('value', '');
      view.propertyDidChange('isInvalid');
      expect(view.get('isInvalid')).to.be.false;
    });

    it("value is ' '", function() {
      view.set('value', ' ');
      view.propertyDidChange('isInvalid');
      expect(view.get('isInvalid')).to.be.false;
    });

    it("value is '2'", function() {
      view.set('value', '2');
      view.propertyDidChange('isInvalid');
      expect(view.get('isInvalid')).to.be.false;
    });
  });
});

describe("App.InputCursorTextfieldView", function() {

  var view;

  beforeEach(function() {
    view = App.InputCursorTextfieldView.create({
      parentView: App.WidgetWizardExpressionView.create({
        expression: {
          data: []
        },
        controller: Em.Object.create({
          updateExpressions: Em.K
        })
      })
    });
    view.removeObserver('value', view, 'validateInput');
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(view, 'focusCursor');
    });
    afterEach(function() {
      view.focusCursor.restore();
    });

    it("focusCursor should be called", function() {
      view.didInsertElement();
      expect(view.focusCursor.calledOnce).to.be.true;
    });
  });

  describe("#focusOut()", function() {

    beforeEach(function() {
      sinon.stub(view, 'saveNumber');
    });
    afterEach(function() {
      view.saveNumber.restore();
    });

    it("saveNumber should be called", function() {
      view.focusOut();
      expect(view.saveNumber.calledOnce).to.be.true;
    });
  });

  describe("#focusCursor()", function() {
    var mock = {
      focus: Em.K
    };

    beforeEach(function() {
      sinon.stub(Em.run, 'next', Em.clb);
      sinon.stub(view, '$').returns(mock);
      sinon.stub(mock, 'focus');
      view.focusCursor();
    });
    afterEach(function() {
      Em.run.next.restore();
      view.$.restore();
      mock.focus.restore();
    });

    it("Em.run.next should be called", function() {
      expect(Em.run.next.calledOnce).to.be.true;
    });

    it("$ should be called", function() {
      expect(view.$.calledTwice).to.be.true;
    });

    it("focus should be called", function() {
      expect(mock.focus.calledOnce).to.be.true;
    });
  });

  describe("#validateInput()", function() {

    beforeEach(function() {
      this.mock = sinon.stub(numberUtils, 'isPositiveNumber');
    });
    afterEach(function() {
      this.mock.restore();
    });

    it("value is positive number", function() {
      this.mock.returns(true);
      view.validateInput();
      expect(view.get('isInvalid')).to.be.false;
    });

    it("value is null", function() {
      this.mock.returns(false);
      view.set('value', null);
      view.validateInput();
      expect(view.get('isInvalid')).to.be.false;
    });

    it("value is empty", function() {
      this.mock.returns(false);
      view.set('value', '');
      view.validateInput();
      expect(view.get('isInvalid')).to.be.false;
    });

    it("value is operator", function() {
      this.mock.returns(false);
      view.set('value', '+');
      view.validateInput();
      expect(view.get('isInvalid')).to.be.false;
      expect(view.get('parentView.expression.data')).to.not.be.empty;
      expect(view.get('value')).to.be.empty;
    });

    it("value is 'm'", function() {
      this.mock.returns(false);
      view.set('value', '+');
      view.validateInput();
      expect(view.get('isInvalid')).to.be.false;
      expect(view.get('value')).to.be.empty;
    });

    it("value is invalid", function() {
      this.mock.returns(false);
      view.set('value', '%');
      view.validateInput();
      expect(view.get('isInvalid')).to.be.true;
    });
  });

  describe("#keyDown()", function() {

    beforeEach(function() {
      sinon.stub(view, 'saveNumber');
    });
    afterEach(function() {
      view.saveNumber.restore();
    });

    it("unexpected key", function() {
      view.set('parentView.expression.data', [{name: '1'}, {name: '2'}]);
      view.keyDown({keyCode: 9});
      expect(view.saveNumber.called).to.be.false;
      expect(view.get('parentView.expression.data.length')).to.be.equal(2);
    });

    it("backspace key and not empty value", function() {
      view.set('value', 'm');
      view.set('parentView.expression.data', [{name: '1'}, {name: '2'}]);
      view.keyDown({keyCode: 8});
      expect(view.saveNumber.called).to.be.false;
      expect(view.get('parentView.expression.data.length')).to.be.equal(2);
    });

    it("backspace key and empty value", function() {
      view.set('value', '');
      view.set('parentView.expression.data', [{name: '1'}, {name: '2'}]);
      view.keyDown({keyCode: 8});
      expect(view.saveNumber.called).to.be.false;
      expect(view.get('parentView.expression.data.length')).to.be.equal(1);
    });

    it("enter key", function() {
      view.set('value', '');
      view.set('parentView.expression.data', [{name: '1'}, {name: '2'}]);
      view.keyDown({keyCode: 13});
      expect(view.saveNumber.called).to.be.true;
      expect(view.get('parentView.expression.data.length')).to.be.equal(2);
    });
  });

  describe("#saveNumber()", function() {

    beforeEach(function() {
      this.mock = sinon.stub(numberUtils, 'isPositiveNumber');
    });
    afterEach(function() {
      this.mock.restore();
    });

    it("value is a positive number", function() {
      view.set('value', '1');
      this.mock.returns(true);
      view.saveNumber();
      expect(view.get('parentView.expression.data').mapProperty('name')).to.be.eql(['1']);
      expect(view.get('numberValue')).to.be.empty;
      expect(view.get('isInvalid')).to.be.false;
      expect(view.get('value')).to.be.empty;
    });

    it("value is not a positive number", function() {
      view.set('value', '-1');
      this.mock.returns(false);
      view.saveNumber();
      expect(view.get('parentView.expression.data')).to.be.empty;
      expect(view.get('value')).to.be.equal('-1');
    });
  });

});

describe('#App.AddMetricExpressionView', function() {
  var AddMetricExpressionView;
  
  beforeEach(function() {
    AddMetricExpressionView = App.AddMetricExpressionView.create({
      currentSelectedComponent: Em.Object.create(),
      controller: Em.Object.create(),
      parentView: Em.Object.create({
        AGGREGATE_FUNCTIONS: ['fun1']
      }),
      elementId: '1'
    });
  });
  
  describe('#metricsSelectionObj.onChangeCallback', function() {
  
    beforeEach(function() {
      AddMetricExpressionView.set('controller.filteredMetrics',  [{
        component_name: 'C1',
        level: 'l1',
        name: 'metric1',
        widget_id: 1
      }]);
      AddMetricExpressionView.set('currentSelectedComponent', Em.Object.create({
        componentName: 'C1',
        serviceName: 'S1',
        level: 'l1',
        hostComponentCriteria: 'criteria1',
        tag: 'tag1',
        selectedAggregation: Em.I18n.t('dashboard.widgets.wizard.step2.aggregateFunction.scanOps')
      }));
      var metricsSelectionObj = AddMetricExpressionView.get('metricsSelectionObj');
      metricsSelectionObj.onChangeCallback({}, {selected: 'metric1'});
    });
    
    it('selectedMetric should be set', function() {
      expect(AddMetricExpressionView.get('currentSelectedComponent.selectedMetric')).to.be.eql(Em.Object.create({
        name: 'metric1',
        hostComponentCriteria: 'criteria1',
        tag: 'tag1',
        componentName: 'C1',
        serviceName: 'S1',
        metricPath: 1,
        isMetric: true
      }));
    });
  
    it('selectedAggregation should be set', function() {
      expect(AddMetricExpressionView.get('currentSelectedComponent.selectedAggregation')).to.be.equal('fun1');
    });
  });
  
  describe('#aggregateFnSelectionObj.onChangeCallback', function() {

    it('selectedAggregation should be set', function() {
      AddMetricExpressionView.set('currentSelectedComponent', Em.Object.create({}));
      var aggregateFnSelectionObj = AddMetricExpressionView.get('aggregateFnSelectionObj');
      aggregateFnSelectionObj.onChangeCallback({}, {selected: 'name1'});
      expect(AddMetricExpressionView.get('currentSelectedComponent.selectedAggregation')).to.be.equal('name1');
    });
  });
  
  describe('#selectComponents', function() {
    var event = {
      context: {},
      stopPropagation: sinon.spy()
    };
    beforeEach(function() {
      AddMetricExpressionView.selectComponents(event);
    });

    it('currentSelectedComponent should be set', function() {
      expect(AddMetricExpressionView.get('currentSelectedComponent')).to.be.an.object;
    });
  
    it('stopPropagation should be called', function() {
      expect(event.stopPropagation.called).to.be.true;
    });
  });
  
  describe('#addMetric', function() {
    var event = {
      context: Em.Object.create({
        selectedMetric: {
          metricPath: 'path',
          name: 'metric1'
        },
        selectedAggregation: 'fun1',
        isAddEnabled: true,
        showAggregateSelect: true
      })
    };
    var data = [];
    beforeEach(function() {
      sinon.stub(AddMetricExpressionView, 'cancel');
      AddMetricExpressionView.set('parentView.expression', {
        data: data
      });
      AddMetricExpressionView.addMetric(event);
    });
    afterEach(function() {
      AddMetricExpressionView.cancel.restore();
    });
    
    it('metric should be added', function() {
      expect(data[0]).to.be.eql(Em.Object.create({
        id: 1,
        metricPath: 'path._fun1',
        name: 'metric1._fun1'
      }));
    });
  
    it('cancel should be called', function() {
      expect(AddMetricExpressionView.cancel.called).to.be.true;
    });
  });
  
  describe('#cancel', function() {
    
    it('selectedAggregation should be set', function() {
      AddMetricExpressionView.cancel();
      expect(AddMetricExpressionView.get('currentSelectedComponent.selectedAggregation')).to.be.equal(
        Em.I18n.t('dashboard.widgets.wizard.step2.aggregateFunction.scanOps')
      );
    });
  
    it('selectedMetric should be null', function() {
      AddMetricExpressionView.cancel();
      expect(AddMetricExpressionView.get('currentSelectedComponent.selectedMetric')).to.be.null;
    });
  });
  
  describe('#getNameServiceGroups', function() {
    beforeEach(function() {
      sinon.stub(App, 'get').returns(true);
      sinon.stub(App.HDFSService, 'find').returns(Em.Object.create({
        masterComponentGroups: [
          {
            name: 'g1'
          }
        ]
      }));
    });
    afterEach(function() {
      App.get.restore();
      App.HDFSService.find.restore();
    });
    
    it('should return service groups', function() {
      expect(AddMetricExpressionView.getNameServiceGroups()).to.be.eql([{
        tag: 'g1',
        displayName: Em.I18n.t('dashboard.widgets.wizard.step2.nameSpaceDropDownItem').format('g1'),
        component: null
      }]);
    });
  });
  
  describe('#getServicesMap', function() {
    beforeEach(function() {
      sinon.stub(App, 'get').returns(true);
      sinon.stub(App.StackServiceComponent, 'find').returns([{
        isMaster: true,
        componentName: 'C2'
      }]);
    });
    afterEach(function() {
      App.get.restore();
      App.StackServiceComponent.find.restore();
    });
  
    it('should return empty when no metrics', function() {
      AddMetricExpressionView.set('controller.filteredMetrics', []);
      expect(AddMetricExpressionView.getServicesMap()).to.be.empty;
    });
  
    it('should return empty when no metrics', function() {
      AddMetricExpressionView.set('controller.filteredMetrics', [
        {
          name: 'metric1',
          component_name: 'NAMENODE',
          level: 'COMPONENT',
          service_name: 'HDFS',
          host_component_criteria: 'criteria1'
        },
        {
          name: 'metric2',
          component_name: 'C1',
          service_name: 'S1',
          level: 'COMPONENT',
          host_component_criteria: 'criteria1'
        },
        {
          name: 'metric3',
          component_name: 'C2',
          service_name: 'S1',
          level: 'COMPONENT',
          host_component_criteria: 'criteria1'
        }
      ]);
      expect(AddMetricExpressionView.getServicesMap()).to.be.eql({
        "S1": {
          "components": {
            "C1": {
              "component_name": "C1",
              "count": 1,
              "hostComponentCriteria": "criteria1",
              "level": "COMPONENT",
              "metrics": [
                "metric2"
              ]
            },
            "C2_COMPONENT": {
              "component_name": "C2",
              "count": 1,
              "hostComponentCriteria": "criteria1",
              "level": "COMPONENT",
              "metrics": [
                "metric3"
              ]
            }
          },
          "count": 2
        }
      });
    });
  });
  
  describe('#componentMap', function() {
    var nameServiceGroups = [{}];
    beforeEach(function() {
      sinon.stub(App, 'get').returns(true);
      sinon.stub(AddMetricExpressionView, 'getServicesMap').returns({
        "S1": {
          "components": {
            "C1": {
              "component_name": "C1",
              "count": 1,
              "hostComponentCriteria": "criteria1",
              "level": "COMPONENT",
              "metrics": [
                "metric2"
              ]
            }
          },
          "count": 1
        },
        "HDFS": {
          "components": {
            "NAMENODE": {
              "component_name": "NAMENODE",
              "count": 1,
              "hostComponentCriteria": "criteria1",
              "level": "COMPONENT",
              "metrics": [
                "metric3"
              ]
            }
          },
          "count": 1
        }
      });
      sinon.stub(AddMetricExpressionView, 'getNameServiceGroups').returns(nameServiceGroups);
      sinon.stub(App.HostComponent, 'getCount').returns(1);
      sinon.stub(AddMetricExpressionView, 'createComponentItem').returns({});
      sinon.stub(AddMetricExpressionView, 'putContextServiceOnTop', function(result) {
        return result;
      });
      sinon.stub(App.StackService, 'find').returns(Em.Object.create({displayName: 'foo'}));
    });
    afterEach(function() {
      App.get.restore();
      AddMetricExpressionView.getServicesMap.restore();
      App.HostComponent.getCount.restore();
      AddMetricExpressionView.getNameServiceGroups.restore();
      AddMetricExpressionView.createComponentItem.restore();
      AddMetricExpressionView.putContextServiceOnTop.restore();
      App.StackService.find.restore();
    });
    
    it('should return map of components', function() {
      AddMetricExpressionView.propertyDidChange('componentMap');
      expect(AddMetricExpressionView.get('componentMap')).to.be.eql([
        Em.Object.create({
          serviceName: 'S1',
          //in order to support panel lists
          href: '#S1',
          displayName: 'foo',
          count: 1,
          components: [{}]
        }),
        Em.Object.create({
          serviceName: 'HDFS',
          //in order to support panel lists
          href: '#HDFS',
          displayName: 'foo',
          count: 1,
          components: [
            Em.Object.create({
              displayName: 'NameNodes',
              isGroup: true,
              components: nameServiceGroups
            })
          ]
        })
      ]);
    });
  });
  
  describe('#createComponentItem', function() {
    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        isMaster: true,
        displayName: 'c1'
      }));
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });
    
    it('should return component', function() {
      var service = {
        "components": {
          "C1": {
            "component_name": "C1",
            "count": 1,
            "hostComponentCriteria": "criteria1",
            "level": "HOSTCOMPONENT",
            "metrics": [
              "metric2"
            ]
          }
        }
      };
      var component = AddMetricExpressionView.createComponentItem(service, 'S1', 'C1', '1', 'tag1');
      component.reopen({
        showAggregateSelect: false,
        isAddEnabled: false
      });
      expect(component).to.be.eql(Em.Object.create({
        componentName: 'C1',
        isAddEnabled: false,
        level: 'HOSTCOMPONENT',
        displayName: Em.I18n.t('widget.create.wizard.step2.activeComponents').format('c1'),
        tag: 'tag1',
        count: 1,
        metrics: ['metric2'],
        selected: false,
        id: 'C11tag1',
        aggregatorId: 'C11_aggregator',
        serviceName: 'S1',
        showAggregateSelect: false,
        selectedMetric: null,
        selectedAggregation: Em.I18n.t('dashboard.widgets.wizard.step2.aggregateFunction.scanOps'),
        hostComponentCriteria: 'criteria1'
      }));
    });
  });
  
  describe('#putContextServiceOnTop', function() {
    
    it('should move service to the top of array', function() {
      AddMetricExpressionView.set('controller.content', {widgetService: 'S2'});
      var serviceComponentMap = [
        {
          serviceName: 'S1'
        },
        {
          serviceName: 'S2'
        }
      ];
      expect(AddMetricExpressionView.putContextServiceOnTop(serviceComponentMap)).to.be.eql([
        {
          serviceName: 'S2'
        },
        {
          serviceName: 'S1'
        }
      ]);
    });
  });
 
});
