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

describe('App.LoadingOverlaySupport', function () {

  beforeEach(function (done) {
    view = Em.View.extend(App.LoadingOverlaySupport, {
      flag: false,
      fieldToObserve: 'flag',
      attributeBindings: ['style'],
      style: 'height: 100px; width: 200px;',
      template: Em.Handlebars.compile('<div class="loading-overlay"></div>'),
      didInsertElement: function () {
        this._super();
        done();
      }
    }).create();
    view.appendTo('#wrapper');
    sinon.stub(window, 'setInterval', Em.clb);
  });

  afterEach(function () {
    view.destroy();
    window.setInterval.restore();
  });

  describe('#doNotChangeFieldToObserve', function () {

    it('should throw an error', function () {
      expect(function () {
        view.set('fieldToObserve', 'somethingNew');
      }).to.throw(Error, 'Do not change `fieldToObserve` after view is initialized');
    });

  });

  describe('#handleFieldChanges', function () {

    describe('overlay is shown', function () {

      beforeEach(function () {
        view.set('flag', true);
      });

      afterEach(function () {
        view.set('flag', false);
      });

      it('should add overlay', function () {
        expect(view.$('.loading-overlay.overlay-visible').length).to.be.equal(1);
      });

      it('overlay width is correct', function () {
        expect(view.$('.loading-overlay.overlay-visible').width()).to.be.equal(200);
      });

      it('overlay height is correct', function () {
        expect(view.$('.loading-overlay.overlay-visible').height()).to.be.equal(100);
      });

    });

    describe('overlay is hidden', function () {

      it('overlay is depends of `flag`-value', function () {
        expect(view.$('.loading-overlay.overlay-visible').length).to.be.equal(0);
        view.set('flag', true);
        expect(view.$('.loading-overlay.overlay-visible').length).to.be.equal(1);
        view.set('flag', false);
        expect(view.$('.loading-overlay.overlay-visible').length).to.be.equal(0);
      });

    });

  });

});
