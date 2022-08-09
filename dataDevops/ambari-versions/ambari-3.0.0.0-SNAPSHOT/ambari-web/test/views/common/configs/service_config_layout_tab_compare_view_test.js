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
var lazyLoading = require('utils/lazy_loading');

describe('App.ServiceConfigLayoutTabCompareView', function () {
  var view;

  beforeEach(function () {
    view = App.ServiceConfigLayoutTabCompareView.create({content: {sectionRows: [
      Em.Object.create({setEach: function () {}}),
      Em.Object.create({setEach: function () {}})
    ]}});
  });

  describe('#onToggleBlock', function () {
    it('should toggle propery isCollapsed of current event.context', function(){
      var event = {context: { toggleProperty: function (){}}};
      sinon.stub(event.context, 'toggleProperty');
      view.onToggleBlock(event);
      expect(event.context.toggleProperty.calledWith('isCollapsed')).to.be.true;
      event.context.toggleProperty.restore();
    });
  });

  describe('#didInsertElement', function () {
    it('should call setEach method of each sectionRow with proper params', function () {
      view.get('content.sectionRows').forEach(function (item) {
        sinon.stub(item, 'setEach');
      });
      view.didInsertElement();
      view.get('content.sectionRows').forEach(function (item) {
        expect(item.setEach.calledTwice).to.be.true;
        item.setEach.restore();
      });
    });
  });
});