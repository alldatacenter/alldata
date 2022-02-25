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

describe('App.PluralizeView', function () {
  var view;
  beforeEach(function () {
    view = App.PluralizeView.create({});
  });

  describe('#getViewPropertyValue', function () {
    beforeEach(function () {
      sinon.stub(Em, 'get');
    });
    afterEach(function () {
      Em.get.restore();
    });
    it('should return controller mainHostDetailsController', function () {
      view.getViewPropertyValue('@controller.mainHostDetailsController');
      expect(Em.get.calledOnce).to.be.true;
      expect(Em.get.calledWith(view, 'controller.mainHostDetailsController')).to.be.true;
    });

    it('should return controller mainHostDetailsView', function () {
      view.getViewPropertyValue('@view.mainHostDetailsView');
      expect(Em.get.calledOnce).to.be.true;
      expect(Em.get.calledWith(view, 'parentView.mainHostDetailsView')).to.be.true;
    });

    it('should return controller mainHostDetailsView', function () {
      view.getViewPropertyValue('view.mainHostDetailsView');
      expect(Em.get.called).to.be.false;
    });
  });

  describe('#tDetect', function () {
    it('should return same word if no : symbol', function () {
      var result = view.tDetect('test');
      expect(result).to.equal('test');
    });

    it('should return same word if first sybol to : is not t', function () {
      var result = view.tDetect('est:test');
      expect(result).to.equal('est');
    });

    it('should return translated word', function () {
      var result = view.tDetect('t:app.name');
      expect(result).to.equal(Em.I18n.t('app.name'));
    });
  });

  describe('#parseValue', function () {
    it('should call getViewPropertyValue if first symbol is @', function () {
      sinon.stub(view, 'getViewPropertyValue');
      view.parseValue('@view.mainHostDetailsView');
      expect(view.getViewPropertyValue.calledOnce).to.be.true;
      expect(view.getViewPropertyValue.calledWith('@view.mainHostDetailsView')).to.be.true;
      view.getViewPropertyValue.restore();
    });

    it('should call getViewPropertyValue if first symbol is t', function () {
      sinon.stub(view, 'tDetect');
      view.parseValue('tview.mainHostDetailsView');
      expect(view.tDetect.calledOnce).to.be.true;
      expect(view.tDetect.calledWith('tview.mainHostDetailsView')).to.be.true;
      view.tDetect.restore();
    });

    it('should not call anything if first symbol is not @ or t', function () {
      sinon.stub(view, 'tDetect');
      sinon.stub(view, 'getViewPropertyValue');
      view.parseValue('view.mainHostDetailsView');
      expect(view.tDetect.called).to.be.false;
      expect(view.getViewPropertyValue.called).to.be.false;
      view.getViewPropertyValue.restore();
      view.tDetect.restore();
    });
  });

  describe('#getWord', function () {
    it('should return singular if count is 1', function () {
      var result = view.getWord(1, 't:app.name', null);
      expect(result).to.be.equal(Em.I18n.t('app.name'));
    });

    it('should return singular if count is 2', function () {
      var result = view.getWord(2, 't:common.component', null);
      expect(result).to.be.equal(Em.I18n.t('common.components'));
    });
  });
});