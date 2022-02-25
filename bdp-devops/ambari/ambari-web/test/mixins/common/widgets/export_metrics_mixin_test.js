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

require('mixins/common/widgets/export_metrics_mixin');
var fileUtils = require('utils/file_utils');
var testHelpers = require('test/helpers');

describe('App.ExportMetricsMixin', function () {

  var obj;

  beforeEach(function () {
    obj = Em.Object.create(App.ExportMetricsMixin);
  });

  describe('#toggleFormatsList', function () {

    var cases = [
      {
        isExportMenuHidden: true,
        title: 'menu should be visible'
      },
      {
        isExportMenuHidden: false,
        title: 'menu should be hidden'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        obj.set('isExportMenuHidden', !item.isExportMenuHidden);
        obj.toggleFormatsList();
        expect(obj.get('isExportMenuHidden')).to.equal(item.isExportMenuHidden);
      });
    });

  });

  describe('#exportGraphData', function () {

    var cases = [
      {
        isExportMenuHidden: true,
        event: {
          context: true
        },
        isCSV: true,
        title: 'CSV, menu should remain hidden'
      },
      {
        isExportMenuHidden: false,
        event: {},
        isCSV: false,
        title: 'JSON, menu should become hidden'
      }
    ];

    beforeEach(function () {
      obj.reopen({
        targetView: {
          ajaxIndex: 'index',
          getDataForAjaxRequest: function () {
            return {
              p: 'v'
            };
          }
        }
      });
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          obj.set('isExportMenuHidden', item.isExportMenuHidden);
          obj.exportGraphData(item.event);
          this.ajaxParams = testHelpers.findAjaxRequest('name', 'index');
        });

        it('isExportMenuHidden is true', function () {
          expect(obj.get('isExportMenuHidden')).to.be.true;
        });
        it('one request was done', function () {
          expect(this.ajaxParams[0]).exists;
        });
        it('ajax-request with correct data', function () {
          expect(this.ajaxParams[0].data).to.eql({
            p: 'v',
            isCSV: item.isCSV
          });
        });
      });
    });

  });

  describe('#exportGraphDataSuccessCallback', function () {

    var cases = [
      {
        response: null,
        showAlertPopupCallCount: 1,
        downloadTextFileCallCount: 0,
        title: 'no response'
      },
      {
        response: {
          metrics: null
        },
        showAlertPopupCallCount: 1,
        downloadTextFileCallCount: 0,
        title: 'no metrics object in response'
      },
      {
        response: {
          metrics: {}
        },
        showAlertPopupCallCount: 1,
        downloadTextFileCallCount: 0,
        title: 'empty metrics object'
      },
      {
        response: {
          metrics: {
            m0: [0, 1]
          }
        },
        params: {
          isCSV: true
        },
        showAlertPopupCallCount: 0,
        downloadTextFileCallCount: 1,
        data: '0,1',
        fileType: 'csv',
        fileName: 'data.csv',
        title: 'export to CSV'
      },
      {
        response: {
          metrics: {
            m0: [0, 1]
          }
        },
        params: {
          isCSV: false
        },
        showAlertPopupCallCount: 0,
        downloadTextFileCallCount: 1,
        data: '[{"name":"m0","data":[0,1]}]',
        fileType: 'json',
        fileName: 'data.json',
        title: 'export to JSON'
      }
    ];

    beforeEach(function () {
      sinon.stub(App, 'showAlertPopup', Em.K);
      sinon.stub(fileUtils, 'downloadTextFile', Em.K);
      sinon.stub(obj, 'prepareCSV').returns('0,1');
      obj.reopen({
        targetView: {
          getData: function (response) {
            var data = [];
            if (response && response.metrics) {
              var name = Em.keys(response.metrics)[0];
              if (name && response.metrics[name]) {
                data = [
                  {
                    name: name,
                    data: response.metrics[name]
                  }
                ];
              }
            }
            return data;
          }
        }
      });
    });

    afterEach(function () {
      App.showAlertPopup.restore();
      fileUtils.downloadTextFile.restore();
      obj.prepareCSV.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          obj.exportGraphDataSuccessCallback(item.response, null, item.params);
        });

        it('downloadTextFile was called needed number of times', function () {
          expect(fileUtils.downloadTextFile.callCount).to.equal(item.downloadTextFileCallCount);
        });

        if (item.downloadTextFileCallCount) {
          it('data is valid', function () {
            expect(fileUtils.downloadTextFile.firstCall.args[0].replace(/\s/g, '')).to.equal(item.data);
          });
          it('fileType is valid', function () {
            expect(fileUtils.downloadTextFile.firstCall.args[1]).to.equal(item.fileType);
          });
          it('fileName is valid', function () {
            expect(fileUtils.downloadTextFile.firstCall.args[2]).to.equal(item.fileName);
          });
        }
      });
    });

  });

  describe('#exportGraphDataErrorCallback', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
    });

    afterEach(function () {
      App.ajax.defaultErrorHandler.restore();
    });

    it('should display error popup', function () {
      obj.exportGraphDataErrorCallback({
          status: 404
        }, null, '', {
          url: 'url',
          type: 'GET'
        });
      expect(App.ajax.defaultErrorHandler.calledOnce).to.be.true;
      expect(App.ajax.defaultErrorHandler.calledWith({
          status: 404
        }, 'url', 'GET', 404)).to.be.true;
    });

  });

  describe('#prepareCSV', function () {

    var cases = [
        {
          displayUnit: 'B',
          result: 'Timestamp,n0 (B),n1 (B)\n1,0,4\n3,2,5\n',
          title: 'display unit set'
        },
        {
          result: 'Timestamp,n0,n1\n1,0,4\n3,2,5\n',
          title: 'display unit not set'
        }
      ],
      data = [
        {
          name: 'n0',
          data: [[0, 1], [2, 3]]
        },
        {
          name: 'n1',
          data: [[4, 1], [5, 3]]
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        obj.reopen({
          targetView: {
            displayUnit: item.displayUnit
          }
        });
        expect(obj.prepareCSV(data)).to.equal(item.result);
      });
    });

  });

  describe('#hideMenuForNoData', function () {

    var cases = [
      {
        isExportButtonHidden: true,
        isExportMenuHidden: true,
        title: 'menu should be hidden'
      },
      {
        isExportButtonHidden: false,
        isExportMenuHidden: false,
        title: 'menu should be visible'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        obj.setProperties({
          isExportButtonHidden: item.isExportButtonHidden,
          isExportMenuHidden: false
        });
        expect(obj.get('isExportMenuHidden')).to.equal(item.isExportMenuHidden);
      });
    });

  });

  describe('#jsonReplacer', function () {

    var cases = [
      {
        json: [
          {
            name: 'n0',
            data: [
              [0, 1],
              [1, 2]
            ]
          }
        ],
        result: '[{"name":"n0","data":[[0,1],[1,2]]}]',
        title: 'valid object'
      },
      {
        json: [
          {
            name: 'n1',
            data: [
              [0, 1],
              [1, 2]
            ],
            p1: 'v1'
          }
        ],
        result: '[{"name":"n1","data":[[0,1],[1,2]]}]',
        title: 'object with redundant property'
      },
      {
        json: [
          {
            name: 'n1',
            data: {
              p2: 'v2'
            }
          }
        ],
        result: '[{"name":"n1","data":{}}]',
        title: 'object with malformed data'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(JSON.stringify(item.json, obj.jsonReplacer())).to.equal(item.result);
      });
    });

  });

});
