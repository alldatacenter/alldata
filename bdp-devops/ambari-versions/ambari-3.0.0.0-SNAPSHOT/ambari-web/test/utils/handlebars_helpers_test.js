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
require('utils/handlebars_helpers');

describe('handlebars_helpers', function () {

  describe('#registerBoundHelper', function () {

    var options = {
      hash: {}
    };

    beforeEach(function () {
      sinon.stub(Em.Handlebars, 'registerHelper', function (name, callback) {
        callback('prop', options);
      });
      sinon.stub(Em.Handlebars.helpers, 'view', Em.K);
      App.registerBoundHelper('helper', {});
    });

    afterEach(function () {
      Em.Handlebars.registerHelper.restore();
      Em.Handlebars.helpers.view.restore();
    });

    it('contentBinding', function () {
      expect(options.hash.contentBinding).to.equal('prop');
    });

    it('view', function () {
      expect(Em.Handlebars.helpers.view.calledOnce).to.be.true;
    });

    it('view arguments', function () {
      expect(Em.Handlebars.helpers.view.firstCall.args).to.eql([{}, options]);
    });

  });

});
