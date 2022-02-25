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
require('views/common/modal_popups/cluster_check_popup');

describe('App.showClusterCheckPopup', function () {

  var isCallbackExecuted,
    callback = function () {
      isCallbackExecuted = true;
    },
    cases = [
      {
        inputData: {
          data: {
            items: [
              {
                UpgradeChecks: {
                  id: 'p0',
                  status: 'PASS'
                }
              },
              {
                UpgradeChecks: {
                  id: 'p1',
                  status: 'PASS'
                }
              }
            ]
          }
        },
        result: {
          primary: Em.I18n.t('common.proceedAnyway'),
          secondary: Em.I18n.t('common.cancel'),
          header: '&nbsp;'
        },
        bodyResult: {
          failTitle: undefined,
          failAlert: undefined,
          warningTitle: undefined,
          warningAlert: undefined,
          fails: [],
          warnings: [],
          hasConfigsMergeConflicts: false,
          isAllPassed: true
        },
        isCallbackExecuted: false,
        title: 'no fails, no warnings, no popup customization'
      },
      {
        inputData: {
          data: {
            items: [
              {
                UpgradeChecks: {
                  id: 'w0',
                  status: 'WARNING',
                  check: 'w0 check',
                  failed_on: 'w0 failed',
                  reason: 'w0 reason'
                }
              },
              {
                UpgradeChecks: {
                  id: 'w1',
                  status: 'WARNING',
                  check: 'w1 check',
                  failed_on: 'w1 failed',
                  reason: 'w1 reason'
                }
              }
            ]
          },
          popup: {
            header: 'checks',
            failTitle: 'fail',
            failAlert: 'something has failed',
            warningTitle: 'warning',
            warningAlert: 'something is not good',
            callback: callback
          }
        },
        result: {
          primary: Em.I18n.t('common.proceedAnyway'),
          secondary: Em.I18n.t('common.cancel'),
          header: 'checks'
        },
        bodyResult: {
          failTitle: 'fail',
          failAlert: 'something has failed',
          warningTitle: 'warning',
          warningAlert: 'something is not good',
          fails: [],
          warnings: [
            {
              check: 'w0 check',
              customView: undefined,
              failed_on: 'w0 failed',
              reason: 'w0 reason'
            },
            {
              check: 'w1 check',
              customView: undefined,
              failed_on: 'w1 failed',
              reason: 'w1 reason'
            }
          ],
          hasConfigsMergeConflicts: false,
          isAllPassed: false
        },
        isCallbackExecuted: true,
        title: 'no fails, default buttons, callback executed'
      },
      {
        inputData: {
          data: {
            items: [
              {
                UpgradeChecks: {
                  id: 'f0',
                  status: 'FAIL',
                  check: 'f0 check',
                  failed_on: 'f0 failed',
                  reason: 'f0 reason'
                }
              },
              {
                UpgradeChecks: {
                  id: 'f1',
                  status: 'FAIL',
                  check: 'f1 check',
                  failed_on: 'f1 failed',
                  reason: 'f1 reason'
                }
              }
            ]
          },
          popup: {
            callback: callback,
            noCallbackCondition: true
          }
        },
        result: {
          primary: Em.I18n.t('common.dismiss'),
          secondary: false,
          header: '&nbsp;'
        },
        bodyResult: {
          failTitle: undefined,
          failAlert: undefined,
          warningTitle: undefined,
          warningAlert: undefined,
          fails: [
            {
              check: 'f0 check',
              customView: undefined,
              failed_on: 'f0 failed',
              reason: 'f0 reason'
            },
            {
              check: 'f1 check',
              customView: undefined,
              failed_on: 'f1 failed',
              reason: 'f1 reason'
            }
          ],
          warnings: [],
          hasConfigsMergeConflicts: false,
          isAllPassed: false
        },
        isCallbackExecuted: false,
        title: 'fails detected, default buttons, callback not executed'
      },
      {
        inputData: {
          data: {
            items: [
              {
                UpgradeChecks: {
                  id: 'p0',
                  status: 'PASS'
                }
              },
              {
                UpgradeChecks: {
                  id: 'p1',
                  status: 'PASS'
                }
              }
            ]
          },
          popup: {
            primary: 'ok',
            secondary: 'cancel'
          },
          configs: [
            {
              name: 'c0',
              wasModified: false
            },
            {
              name: 'c1',
              wasModified: true
            }
          ]
        },
        result: {
          primary: 'ok',
          secondary: 'cancel',
          header: '&nbsp;'
        },
        bodyResult: {
          failTitle: undefined,
          failAlert: undefined,
          warningTitle: undefined,
          warningAlert: undefined,
          fails: [],
          warnings: [],
          hasConfigsMergeConflicts: true,
          hasConfigsRecommendations: true,
          isAllPassed: false
        },
        configsResult: [
          {
            name: 'c0',
            wasModified: false
          }
        ],
        configRecommendResult: [
          {
            name: 'c1',
            wasModified: true
          }
        ],
        isCallbackExecuted: false,
        title: 'configs merge conflicts detected, custom buttons'
      }
    ];

  beforeEach(function () {
    isCallbackExecuted = false;
    sinon.stub(App, 'tooltip', Em.K);
  });

  afterEach(function () {
    App.tooltip.restore();
  });

  cases.forEach(function (item) {

    describe(item.title, function () {

      var popup;
      var popupBody;

      beforeEach(function () {
        popup = App.showClusterCheckPopup(item.inputData.data, item.inputData.popup, item.inputData.configs);
        popupBody = popup.bodyClass.create();
        popup.onPrimary();
      });

      describe('result', function () {
        Em.keys(item.result).forEach(function (key) {
          it(key, function () {
            expect(popup[key]).to.equal(item.result[key]);
          });
        });
      });

      describe('bodyResult', function () {
        Em.keys(item.bodyResult).forEach(function (key) {
          it(key, function () {
            expect(popupBody[key]).to.eql(item.bodyResult[key]);
          });
        });
      });

      it('callbackExecuted', function () {
        expect(isCallbackExecuted).to.equal(item.isCallbackExecuted);
      });

      if (item.bodyResult.hasConfigsMergeConflicts) {
        it('hasConfigsMergeConflicts = true', function () {
          var configsMergeTable = popupBody.configsMergeTable.create();
          expect(configsMergeTable.configs).to.eql(item.configsResult);
        });
      }

      if (item.bodyResult.hasConfigsRecommendations) {
        it('hasConfigsRecommendations = true', function () {
          var configsRecommendTable = popupBody.configsRecommendTable.create();
          expect(configsRecommendTable.configs).to.eql(item.configRecommendResult);
        });
      }

    });
  });

});
