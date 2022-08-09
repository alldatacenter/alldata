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

require('mixins/main/service/configs/widget_popover_support');

var mixin;

describe('App.WidgetPopoverSupport', function () {

  beforeEach(function() {
    mixin = Em.Object.create(App.WidgetPopoverSupport, {
      stepConfigs: [],
      content: Em.Object.create(),
      on: Em.K,
      $: Em.K
    });
  });

  describe("#popoverPlacement", function () {

    var testCases = [
      {
        sectionLastColumn: false,
        subSectionLastColumn: false,
        expected: 'right'
      },
      {
        sectionLastColumn: true,
        subSectionLastColumn: false,
        expected: 'right'
      },
      {
        sectionLastColumn: false,
        subSectionLastColumn: true,
        expected: 'right'
      },
      {
        sectionLastColumn: true,
        subSectionLastColumn: true,
        expected: 'left'
      }
    ];

    testCases.forEach(function(test) {
      it("subSection.isLastColumn = " + test.subSectionLastColumn +
         "section.isLastColumn = " + test.sectionLastColumn, function() {
        mixin.setProperties({
          section: {
            isLastColumn: test.sectionLastColumn
          },
          subSection: {
            isLastColumn: test.subSectionLastColumn
          }
        });
        mixin.propertyDidChange('popoverPlacement');
        expect(mixin.get('popoverPlacement')).to.be.equal(test.expected);
      });
    });
  });

  describe("#initPopover()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'destroyPopover');
      sinon.stub(App, 'popover');
      sinon.stub(mixin, 'on');
      sinon.stub(mixin, '$')
    });

    afterEach(function() {
      mixin.on.restore();
      App.popover.restore();
      mixin.destroyPopover.restore();
      mixin.$.restore();
    });

    it("destroyPopover should not be called", function() {
      mixin.set('isPopoverEnabled', false);
      mixin.initPopover();
      expect(mixin.destroyPopover.called).to.be.false;
    });

    it("App.popover should not be called", function() {
      mixin.set('isPopoverEnabled', false);
      mixin.initPopover();
      expect(App.popover.called).to.be.false;
    });

    it("on should not be called", function() {
      mixin.set('isPopoverEnabled', false);
      mixin.initPopover();
      expect(mixin.on.called).to.be.false;
    });

    it("destroyPopover should be called", function() {
      mixin.set('isPopoverEnabled', true);
      mixin.initPopover();
      expect(mixin.destroyPopover.calledOnce).to.be.true;
    });

    it("App.popover should be called", function() {
      mixin.set('isPopoverEnabled', true);
      mixin.initPopover();
      expect(App.popover.calledOnce).to.be.true;
    });

    it("on should be called", function() {
      mixin.set('isPopoverEnabled', true);
      mixin.initPopover();
      expect(mixin.on.calledWith('willDestroyElement', mixin, mixin.destroyPopover)).to.be.true;
    });
  });

  describe("#destroyPopover()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'movePopover');
    });

    afterEach(function() {
      mixin.movePopover.restore();
    });

    it("movePopover should be called", function() {
      mixin.destroyPopover();
      expect(mixin.movePopover.calledWith('destroy')).to.be.true;
    });
  });

  describe("#hidePopover()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'movePopover');
    });

    afterEach(function() {
      mixin.movePopover.restore();
    });

    it("movePopover should be called", function() {
      mixin.hidePopover();
      expect(mixin.movePopover.calledWith('hide')).to.be.true;
    });
  });

  describe("#movePopover()", function () {
    var mock = {
      popover: Em.K
    };

    beforeEach(function() {
      sinon.stub(mixin, '$').returns(mock);
      sinon.stub(mock, 'popover');
    });

    afterEach(function() {
      mock.popover.restore();
      mixin.$.restore();
    });

    it("popover should be called", function() {
      mixin.movePopover('action');
      expect(mock.popover.calledWith('action')).to.be.true;
    });
  });

});