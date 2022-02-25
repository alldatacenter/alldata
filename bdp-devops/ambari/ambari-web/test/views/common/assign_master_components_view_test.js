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

describe('App.AssignMasterComponentsView', function () {
  var view;
  var controllerMock = {
    loadStep: function () {
    },
    clearStepOnExit: function () {
    }
  };

  beforeEach(function () {
    view = App.AssignMasterComponentsView.create();
    sinon.stub(view, 'get').returns(controllerMock);
    sinon.stub(controllerMock, 'loadStep');
    sinon.stub(controllerMock, 'clearStepOnExit');
  });

  afterEach(function () {
    view.get.restore();
    controllerMock.loadStep.restore();
    controllerMock.clearStepOnExit.restore();
  });

  describe('#didInsertElement', function () {
    it('should call load step controller method', function () {
      view.didInsertElement();
      expect(controllerMock.loadStep.calledOnce).to.be.true;
    });
  });

  describe('#willDestroyElement', function () {
    it('should call clearStepOnExit controller method', function () {
      view.willDestroyElement();
      expect(controllerMock.clearStepOnExit.calledOnce).to.be.true;
    });
  });
});

describe('#InputHostView', function () {
  var view;

  beforeEach(function () {
    view = App.InputHostView.create();
    view.$ = function () {
      return {
        parent: function () {
          return {
            removeClass: function () {
            },
            addClass: function () {
            }
          }
        }
      }
    }
  });

  describe('#changeHandler', function () {
    it('should call shouldChangeHandlerBeCalled and if it returns false should do anything', function () {
      sinon.stub(view, 'shouldChangeHandlerBeCalled').returns(false);
      sinon.stub(Em, 'none');
      view.set('controller', Em.Object.create({
        shouldChangeHandlerBeCalled: true,
        hosts: []
      }));
      expect(view.shouldChangeHandlerBeCalled.called).to.be.true;
      expect(Em.none.called).to.be.false;
      view.shouldChangeHandlerBeCalled.restore();
      Em.none.restore();
    });

    it('should call updateIsHostNameValidFlag and updateIsSubmitDisabled if host is none', function () {
      view.set('value', 'host3');
      view.set('component', Em.Object.create({
        component_name: 'HIVE',
        serviceComponentId: 1,
        isHostNameValid: true
      }));
      var controller = Em.Object.create({
        shouldChangeHandlerBeCalled: true,
        hosts: [Em.Object.create({host_name: 'host1'}), Em.Object.create({host_name: 'host2'})],
        updateIsHostNameValidFlag: function () {
        },
        updateIsSubmitDisabled: function () {
        },
        isHostNameValid: true
      });
      sinon.stub(view, 'shouldChangeHandlerBeCalled').returns(true);
      sinon.stub(controller, 'updateIsHostNameValidFlag');
      sinon.stub(controller, 'updateIsSubmitDisabled');
      view.set('controller', controller);
      expect(controller.updateIsHostNameValidFlag.calledWith('HIVE', 1)).to.be.true;
      expect(controller.updateIsSubmitDisabled.called).to.be.true;
      view.shouldChangeHandlerBeCalled.restore();
      controller.updateIsHostNameValidFlag.restore();
      controller.updateIsSubmitDisabled.restore();
    });

    it('should call tryTriggerRebalanceForMultipleComponents if host is defined', function () {
      view.set('value', 'host1');
      view.set('component', Em.Object.create({
        component_name: 'HIVE',
        serviceComponentId: 1,
        isHostNameValid: true
      }));
      var controller = Em.Object.create({
        shouldChangeHandlerBeCalled: true,
        hosts: [Em.Object.create({host_name: 'host1'}), Em.Object.create({host_name: 'host2'})],
        assignHostToMaster: function () {
        },
        updateIsSubmitDisabled: function () {
        },
        isHostNameValid: true
      });
      sinon.stub(view, 'shouldChangeHandlerBeCalled').returns(true);
      sinon.stub(controller, 'assignHostToMaster');
      sinon.stub(controller, 'updateIsSubmitDisabled');
      sinon.stub(view, 'tryTriggerRebalanceForMultipleComponents');
      view.set('controller', controller);
      expect(view.tryTriggerRebalanceForMultipleComponents.called).to.be.true;
      expect(controller.updateIsSubmitDisabled.called).to.be.true;
      expect(controller.assignHostToMaster.calledWith('HIVE', 'host1', 1)).to.be.true;
      view.shouldChangeHandlerBeCalled.restore();
      controller.assignHostToMaster.restore();
      controller.updateIsSubmitDisabled.restore();
      view.tryTriggerRebalanceForMultipleComponents.restore();
    });
  });

  describe('#initContent', function () {
    it('should call updateTypeaheadData with proper params', function () {
      var hosts = [{
        host_info: 'test1'
      }, {
        host_info: 'test2'
      }];
      sinon.stub(view, 'updateTypeaheadData');
      sinon.stub(view, 'getAvailableHosts').returns(hosts);
      view.initContent();
      expect(view.updateTypeaheadData.calledWith(['test1', 'test2'])).to.be.true;
      view.updateTypeaheadData.restore();
      view.getAvailableHosts.restore();
    });
  });
});

describe('#SelectHostView', function () {
  beforeEach(function () {
    view = App.SelectHostView.create();
    view.$ = function () {
      return {
        parent: function () {
          return {
            removeClass: function () {
            },
            addClass: function () {
            }
          }
        }
      }
    }
  });

  describe('#didInsertElement', function () {
    beforeEach(function () {
      sinon.stub(App, 'popover');
    });

    afterEach(function () {
      App.popover.restore();
    });

    it('should call initContent method', function () {
      sinon.stub(view, 'initContent');
      view.didInsertElement();
      expect(view.initContent.calledOnce).to.be.true;
      view.initContent.restore();
    });

    it('should set a correct value', function () {
      sinon.stub(view, 'initContent');
      view.set('component', Em.Object.create({selectedHost: 'host1'}));
      view.didInsertElement();
      expect(view.get('value')).to.be.equal('host1');
      view.initContent.restore();
    });
  });

  describe('#changeHandler', function () {
    it('should call shouldChangeHandlerBeCalled and if it returns false should do anything', function () {
      sinon.stub(view, 'shouldChangeHandlerBeCalled').returns(false);
      sinon.stub(view, 'tryTriggerRebalanceForMultipleComponents');
      view.set('controller', Em.Object.create({
        shouldChangeHandlerBeCalled: true,
        hosts: []
      }));
      expect(view.shouldChangeHandlerBeCalled.called).to.be.true;
      expect(view.tryTriggerRebalanceForMultipleComponents.called).to.be.false;
      view.shouldChangeHandlerBeCalled.restore();
      view.tryTriggerRebalanceForMultipleComponents.restore();
    });

    it('should call tryTriggerRebalanceForMultipleComponents if host is defined', function () {
      view.set('value', 'host1');
      view.set('component', Em.Object.create({
        component_name: 'HIVE',
        serviceComponentId: 1,
        isHostNameValid: true
      }));
      var controller = Em.Object.create({
        shouldChangeHandlerBeCalled: true,
        hosts: [Em.Object.create({host_name: 'host1'}), Em.Object.create({host_name: 'host2'})],
        assignHostToMaster: function () {
        },
        updateIsSubmitDisabled: function () {
        },
        isHostNameValid: true
      });
      sinon.stub(view, 'shouldChangeHandlerBeCalled').returns(true);
      sinon.stub(controller, 'assignHostToMaster');
      sinon.stub(view, 'tryTriggerRebalanceForMultipleComponents');
      view.set('controller', controller);
      expect(view.tryTriggerRebalanceForMultipleComponents.called).to.be.true;
      expect(controller.assignHostToMaster.calledWith('HIVE', 'host1', 1)).to.be.true;
      view.shouldChangeHandlerBeCalled.restore();
      controller.assignHostToMaster.restore();
      view.tryTriggerRebalanceForMultipleComponents.restore();
    });
  });
});