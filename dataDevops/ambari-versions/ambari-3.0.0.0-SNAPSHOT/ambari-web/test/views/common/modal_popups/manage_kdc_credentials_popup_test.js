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
require('views/common/modal_popups/manage_kdc_credentials_popup');

describe('App.showManageCredentialsPopup', function () {
  var view;
  
  describe('#onPrimary', function () {
    
    beforeEach(function () {
      view = App.showManageCredentialsPopup();
    });

    it('saveKDCCredentials should be called', function () {
      var formView = {
        saveKDCCredentials: sinon.stub().returns({
          always: Em.clb
        })
      };
      view.reopen({
        formView: formView
      });
      sinon.stub(view, 'hide');
      
      view.onPrimary();
      expect(formView.saveKDCCredentials.called).to.be.true;
  
      view.hide.restore();
    });
  
    it('hide should be called', function () {
      sinon.stub(view, 'hide');
    
      view.onPrimary();
      expect(view.hide.calledOnce).to.be.true;
    
      view.hide.restore();
    });
  });
  
  describe('#onThird', function () {
    
    beforeEach(function () {
      view = App.showManageCredentialsPopup();
    });
    
    it('removeKDCCredentials should be called', function () {
      var formView = {
        removeKDCCredentials: sinon.stub().returns({deferred: {
          always: Em.clb
        }})
      };
      view.reopen({
        formView: formView
      });
      sinon.stub(view, 'hide');
      
      view.onThird();
      expect(formView.removeKDCCredentials.called).to.be.true;
      
      view.hide.restore();
    });
  });
});
