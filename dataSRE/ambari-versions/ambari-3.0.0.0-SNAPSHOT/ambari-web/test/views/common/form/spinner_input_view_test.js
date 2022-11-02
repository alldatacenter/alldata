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
var e;

function getView() {
  return App.SpinnerInputView.create({});
}

describe('App.SpinnerInputView', function () {

  beforeEach(function () {
    view = getView();
    e = {
      preventDefault: Em.K
    };
    sinon.spy(e, 'preventDefault');
  });

  afterEach(function () {
    e.preventDefault.restore();
  });

  App.TestAliases.testAsComputedOr(getView(), 'computedDisabled', ['!content.enabled', 'disabled']);

  describe('#keyDown', function () {

    Em.A([
      {
        charCode: 46,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 8,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 9,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 27,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 13,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 110,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 65,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 67,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 88,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 35,
        ctrlKey: true,
        e: {
          preventDefault: false
        }
      }
    ]).forEach(function (test) {
      it('charCode: ' + test.charCode + ', ctrlKey: ' + test.ctrlKey, function () {
        e.charCode = test.charCode;
        e.ctrlKey = test.ctrlKey;
        view.keyDown(e);
        expect(e.preventDefault.called).to.equal(test.e.preventDefault);
      });
    });

    Em.A([
      {
        charCode: 35,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 36,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 37,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 38,
        e: {
          preventDefault: false
        }
      },
      {
        charCode: 39,
        e: {
          preventDefault: false
        }
      }
    ]).forEach(function (test) {
      it('charCode: ' + test.charCode, function () {
        e.charCode = test.charCode;
        view.keyDown(e);
        expect(e.preventDefault.called).to.equal(test.e.preventDefault);
      });
    });

    Em.A([
      {
        charCode: 190,
        shiftKey: false,
        e: {
          preventDefault: true
        }
      },
      {
        charCode: 190,
        shiftKey: true,
        e: {
          preventDefault: true
        }
      }
    ]).forEach(function (test) {
      it('charCode: ' + test.charCode + ', shiftKey: ' + test.shiftKey, function () {
        e.charCode = test.charCode;
        e.shiftKey = test.shiftKey;
        view.keyDown(e);
        expect(e.preventDefault.calledOnce).to.equal(test.e.preventDefault);
      });
    });

  });

});
