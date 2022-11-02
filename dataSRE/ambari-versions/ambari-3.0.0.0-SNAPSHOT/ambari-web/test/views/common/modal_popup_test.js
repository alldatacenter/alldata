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
require('views/common/modal_popup');

describe('App.ModalPopup', function() {

  var popup;

  beforeEach(function () {
    popup = App.ModalPopup.create(
      {
        primary: 'test',
        secondary: 'test',
        header: 'test',
        body: '<p>text</p><input type="text"><input type="checkbox"><input type="button">',
        $: function () {
          return $(this);
        }
      }
    );
  });

  describe('#didInsertElement', function () {

    beforeEach(function () {
      this.spy = sinon.spy(popup, "focusElement");
    });

    afterEach(function () {
      this.spy.restore();
    });

    it('should focus on the first input element', function () {
      popup.didInsertElement();
      expect(this.spy.called).to.be.true;
    });
  });

  describe('#escapeKeyPressed', function () {

    var returnedValue,
      event = {
        preventDefault: Em.K,
        stopPropagation: Em.K
      },
      cases = [
        {
          buttons: [],
          preventDefaultCallCount: 0,
          stopPropagationCallCount: 0,
          clickCallCount: 0,
          returnedValue: undefined,
          title: 'no close button'
        },
        {
          buttons: [{}],
          preventDefaultCallCount: 1,
          stopPropagationCallCount: 1,
          clickCallCount: 1,
          returnedValue: false,
          title: 'close button available'
        }
      ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          item.buttons.click = Em.K;
          sinon.stub(popup, '$').returns({
            find: function () {
              return {
                last: function () {
                  return item.buttons;
                }
              }
            }
          });
          sinon.spy(item.buttons, 'click');
          sinon.spy(event, 'preventDefault');
          sinon.spy(event, 'stopPropagation');
          returnedValue = popup.escapeKeyPressed(event);
        });

        afterEach(function () {
          popup.$.restore();
          item.buttons.click.restore();
          event.preventDefault.restore();
          event.stopPropagation.restore();
        });

        it('prevent default behaviour', function () {
          expect(event.preventDefault.callCount).to.equal(item.preventDefaultCallCount);
        });

        it('stop event propagation', function () {
          expect(event.stopPropagation.callCount).to.equal(item.stopPropagationCallCount);
        });

        it('close button click', function () {
          expect(item.buttons.click.callCount).to.equal(item.clickCallCount);
        });

        it('returned value', function () {
          expect(returnedValue).to.equal(item.returnedValue);
        });

      });

    });

  });

  describe('#enterKeyPressed', function () {

    var returnedValue,
      event = {
        preventDefault: Em.K,
        stopPropagation: Em.K
      },
      cases = [
        {
          buttons: [],
          isTextArea: true,
          preventDefaultCallCount: 0,
          stopPropagationCallCount: 0,
          clickCallCount: 0,
          returnedValue: undefined,
          title: 'focus on textarea, no primary button'
        },
        {
          buttons: [],
          isTextArea: false,
          preventDefaultCallCount: 0,
          stopPropagationCallCount: 0,
          clickCallCount: 0,
          returnedValue: undefined,
          title: 'no focus on textarea, no primary button'
        },
        {
          buttons: [{}],
          isTextArea: true,
          disabled: 'disabled',
          preventDefaultCallCount: 0,
          stopPropagationCallCount: 0,
          clickCallCount: 0,
          returnedValue: undefined,
          title: 'focus on textarea, primary button disabled'
        },
        {
          buttons: [{}],
          isTextArea: false,
          disabled: 'disabled',
          preventDefaultCallCount: 0,
          stopPropagationCallCount: 0,
          clickCallCount: 0,
          returnedValue: undefined,
          title: 'no focus on textarea, primary button disabled'
        },
        {
          buttons: [{}],
          isTextArea: true,
          disabled: '',
          preventDefaultCallCount: 0,
          stopPropagationCallCount: 0,
          clickCallCount: 0,
          returnedValue: undefined,
          title: 'focus on textarea, primary button enabled'
        },
        {
          buttons: [{}],
          isTextArea: false,
          disabled: '',
          preventDefaultCallCount: 1,
          stopPropagationCallCount: 1,
          clickCallCount: 1,
          returnedValue: false,
          title: 'no focus on textarea, primary button enabled'
        }
      ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          item.buttons.click = Em.K;
          item.buttons.attr = function () {
            return item.disabled;
          };
          sinon.stub(popup, '$').returns({
            find: function () {
              return {
                last: function () {
                  return item.buttons;
                }
              }
            }
          });
          sinon.stub(window, '$').withArgs('*:focus').returns({
            is: function () {
              return item.isTextArea
            }
          });
          sinon.spy(item.buttons, 'click');
          sinon.spy(event, 'preventDefault');
          sinon.spy(event, 'stopPropagation');
          returnedValue = popup.enterKeyPressed(event);
        });

        afterEach(function () {
          popup.$.restore();
          window.$.restore();
          item.buttons.click.restore();
          event.preventDefault.restore();
          event.stopPropagation.restore();
        });

        it('prevent default behaviour', function () {
          expect(event.preventDefault.callCount).to.equal(item.preventDefaultCallCount);
        });

        it('stop event propagation', function () {
          expect(event.stopPropagation.callCount).to.equal(item.stopPropagationCallCount);
        });

        it('primary button click', function () {
          expect(item.buttons.click.callCount).to.equal(item.clickCallCount);
        });

        it('returned value', function () {
          expect(returnedValue).to.equal(item.returnedValue);
        });

      });

    });

  });

});