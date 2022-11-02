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
require('views/wizard/step3/hostWarningPopupFooter_view');
var view;

describe('App.WizardStep3HostWarningPopupFooter', function() {

  beforeEach(function() {
    view = App.WizardStep3HostWarningPopupFooter.create();
    view.reopen({footerController: Em.Object.create()});
  });

  describe('#progressWidth', function() {
    it('based on footerController.checksUpdateProgress', function() {
      view.set('footerController.checksUpdateProgress', 42);
      expect(view.get('progressWidth')).to.equal('width:42%');
    });
  });

  describe('#isUpdateInProgress', function() {
    var tests = Em.A([
      {checksUpdateProgress: 0, checkHostFinished: true, e: false},
      {checksUpdateProgress: 100, checkHostFinished: true, e: false},
      {checksUpdateProgress: 50, checkHostFinished: true, e: true},
      {checksUpdateProgress: 0, checkHostFinished: false, e: true},
      {checksUpdateProgress: 100, checkHostFinished: false, e: true},
      {checksUpdateProgress: 50, checkHostFinished: false, e: true}
    ]);
    tests.forEach(function(test) {
      it(test.checksUpdateProgress, function() {
        view.setProperties({
          'checkHostFinished': test.checkHostFinished,
          'footerController.checksUpdateProgress': test.checksUpdateProgress
        });
        expect(view.get('isUpdateInProgress')).to.equal(test.e);
      });
    });
  });

  describe('#updateStatusClass', function() {
    var tests = Em.A([
      {checksUpdateStatus: 'SUCCESS', e: 'text-success'},
      {checksUpdateStatus: 'FAILED', e: 'text-danger'},
      {checksUpdateStatus: 'PANIC', e: null}
    ]);
    tests.forEach(function(test) {
      it(test.checksUpdateStatus, function() {
        view.set('footerController.checksUpdateStatus', test.checksUpdateStatus);
        if (Em.isNone(test.e)) {
          expect(view.get('updateStatusClass')).to.be.null;
        }
        else {
          expect(view.get('updateStatusClass')).to.equal(test.e);
        }
      })
    });
  });

  describe('#updateStatus', function() {
    var tests = Em.A([
      {checksUpdateStatus: 'SUCCESS', e: Em.I18n.t('installer.step3.warnings.updateChecks.success')},
      {checksUpdateStatus: 'FAILED', e: Em.I18n.t('installer.step3.warnings.updateChecks.failed')},
      {checksUpdateStatus: 'PANIC', e: null}
    ]);
    tests.forEach(function(test) {
      it(test.checksUpdateStatus, function() {
        view.set('footerController.checksUpdateStatus', test.checksUpdateStatus);
        if (Em.isNone(test.e)) {
          expect(view.get('updateStatus')).to.be.null;
        }
        else {
          expect(view.get('updateStatus')).to.equal(test.e);
        }
      })
    });
  });

});
