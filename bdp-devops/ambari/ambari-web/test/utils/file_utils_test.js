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

var fileUtils = require('utils/file_utils');

describe('file_utils', function () {

  describe('#openInfoInNewTab', function () {

    var mock = {
      document: {
        write: Em.K
      },
      focus: Em.K
    };

    beforeEach(function () {
      sinon.stub(window, 'open').returns(mock);
      sinon.spy(mock.document, 'write');
      sinon.spy(mock, 'focus');
      fileUtils.openInfoInNewTab('data');
    });

    afterEach(function () {
      window.open.restore();
      mock.document.write.restore();
      mock.focus.restore();
    });

    it('opening new window', function () {
      expect(window.open.calledOnce).to.be.true;
    });

    it('no URL for new window', function () {
      expect(window.open.firstCall.args).to.eql(['']);
    });

    it('writing document contents', function () {
      expect(mock.document.write.calledOnce).to.be.true;
    });

    it('document contents', function () {
      expect(mock.document.write.firstCall.args).to.eql(['data']);
    });

    it('focusing on new window', function () {
      expect(mock.focus.calledOnce).to.be.true;
    });

  });

  describe('#safariDownload', function () {

    var linkEl = {
      click: Em.K
    };

    beforeEach(function () {
      sinon.stub(document, 'createElement').returns(linkEl);
      sinon.stub(document.body, 'appendChild', Em.K);
      sinon.stub(document.body, 'removeChild', Em.K);
      sinon.spy(linkEl, 'click');
      fileUtils.safariDownload('file data', 'csv', 'file.csv');
    });

    afterEach(function () {
      document.createElement.restore();
      document.body.appendChild.restore();
      document.body.removeChild.restore();
      linkEl.click.restore();
    });

    it('creating new element', function () {
      expect(document.createElement.calledOnce).to.be.true;
    });

    it('new element is a link', function () {
      expect(document.createElement.firstCall.args).to.eql(['a']);
    });

    it('link URL', function () {
      expect(linkEl.href).to.equal('data:attachment/csv;charset=utf-8,file%20data');
    });

    it('file name', function () {
      expect(linkEl.download).to.equal('file.csv');
    });

    it('appending element to document', function () {
      expect(document.body.appendChild.calledOnce).to.be.true;
    });

    it('link is appended', function () {
      expect(document.body.appendChild.firstCall.args).to.eql([linkEl]);
    });

    it('link is clicked', function () {
      expect(linkEl.click.calledOnce).to.be.true;
    });

    it('removing element from document', function () {
      expect(document.body.removeChild.calledOnce).to.be.true;
    });

    it('link is removed', function () {
      expect(document.body.removeChild.firstCall.args).to.eql([linkEl]);
    });

  });

});
