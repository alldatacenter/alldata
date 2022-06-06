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

describe('App.NotScrollableTextArea', function() {
  var view;
  var $ = {select: function () {}, val: function () {}, height: function () {}};
  beforeEach(function () {
    view = App.NotScrollableTextArea.create({});
    sinon.stub($, 'select');
    sinon.stub($, 'height');
    sinon.stub(view, '$').returns($);
  });

  afterEach(function () {
    $.select.restore();
    $.height.restore();
    view.$.restore();
  });

  describe('#didInsertElement', function () {
    it('should call fitHeight and select methods', function () {
      sinon.stub(view, 'fitHeight');
      view.didInsertElement();
      expect(view.fitHeight.called).to.be.true;
      expect($.select.called).to.be.true;
      view.fitHeight.restore();
    });
  });

  describe('#fitHeight', function () {
    it('should run next height', function () {
      sinon.stub(Em.run, 'next');
      view.set('value', 1);
      expect(Em.run.next.called).to.be.true;
      Em.run.next.restore();
    });
  });
});