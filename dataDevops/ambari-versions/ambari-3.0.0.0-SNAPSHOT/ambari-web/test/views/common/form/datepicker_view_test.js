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

describe('#DatepickerFieldView', function () {
  var view;
  beforeEach(function () {
    view = App.DatepickerFieldView.create({});
  });

  describe('#didInsertElement', function () {
    it('should create datetimepicker', function () {
      var $ = {datetimepicker: function(){}, on: function(){}};
      sinon.stub($, 'datetimepicker');
      sinon.stub(view, '$').returns($);
      view.didInsertElement();
      expect($.datetimepicker.calledOnce).to.be.true;
      $.datetimepicker.restore();
      view.$.restore();
    });

    it('should create event listener on change event with view.onChangeDate callback', function () {
      var $ = {datetimepicker: function(){}, on: function(){}};
      var f = function () {};
      sinon.stub($, 'on');
      sinon.stub(view, '$').returns($);
      sinon.stub(view.onChangeDate, 'bind').returns(f);
      view.didInsertElement();
      expect($.on.calledOnce).to.be.true;
      expect($.on.calledWith('changeDate', f)).to.be.true;
      $.on.restore();
      view.$.restore();
      view.onChangeDate.bind.restore();
    });
  });

  describe('#onChangeDate', function () {
    it('should set value property using event object correct', function () {
      view.onChangeDate({target: {value: '05/10/2018'}});
      expect(view.get('value')).to.eql('05/10/2018');
    });
  });
});